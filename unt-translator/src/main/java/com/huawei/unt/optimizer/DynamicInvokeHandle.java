/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.NEW_OBJ;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import static sootup.core.jimple.common.constant.MethodHandle.Kind.REF_INVOKE_CONSTRUCTOR;
import static sootup.core.jimple.common.constant.MethodHandle.Kind.REF_INVOKE_STATIC;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;

import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.constant.MethodType;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.types.JavaClassType;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * DynamicInvoke stmt translator
 *
 * @since 2025-06-30
 */
public class DynamicInvokeHandle implements Optimizer {
    private static final String TAB_INLINE = TAB + TAB + TAB;
    private static final String OBJ_TRANS = TAB_INLINE + "%1$s *in%2$d = (%1$s*) %3$s;" + NEW_LINE;
    private static final String UNKNOWN_PUT_REF = TAB_INLINE + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB_INLINE + TAB + "%1$s->putRefCount();" + NEW_LINE
            + TAB_INLINE + "}" + NEW_LINE;
    private static final String GET_REF = TAB_INLINE + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB_INLINE + TAB + "%1$s->getRefCount();" + NEW_LINE + TAB + "}" + NEW_LINE;

    private ClassType declClassType;
    private TranslatorValueVisitor valueVisitor;
    private CallerKind callerKind;
    private MethodSignature implementedInterfaceSignature;

    @Override
    public boolean fetch(MethodContext methodContext) {
        return !methodContext.getStmts().isEmpty();
    }

    @Override
    public void optimize(MethodContext methodContext) {
        declClassType = methodContext.getJavaMethod().getDeclClassType();
        valueVisitor = new TranslatorValueVisitor(methodContext);
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            Stmt stmt = methodContext.getStmts().get(i);
            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getRightOp() instanceof JDynamicInvokeExpr) {
                JDynamicInvokeExpr dynamicInvokeExpr = (JDynamicInvokeExpr) ((JAssignStmt) stmt).getRightOp();
                List<Immediate> args = dynamicInvokeExpr.getArgs().stream().collect(Collectors.toList());
                try {
                    OptimizedValue optimizedValue = new OptimizedValue(
                            String.format(NEW_OBJ,
                                    TranslatorUtils.formatType(((JAssignStmt) stmt).getLeftOp().getType()),
                                    getLambdaCodes(dynamicInvokeExpr, args)),
                            dynamicInvokeExpr);
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(
                                    ((JAssignStmt) stmt).getLeftOp(), optimizedValue, (JAssignStmt) stmt));
                } catch (TranslatorException e) {
                    throw new TranslatorException(String.format(
                            "the dynamic invoke stmt is limited supported now,"
                                    + " the method %s has unsupported dynamic invoke stmt! ",
                            methodContext.getJavaMethod().getSignature()) + e.getMessage());
                }
            }
        }
    }

    private String getLambdaCodes(JDynamicInvokeExpr expr, List<Immediate> args) {
        StringBuilder lambdaCodes = new StringBuilder();
        List<Immediate> bootstrapArgs = expr.getBootstrapArgs();
        try {
            MethodType declMethodType = (MethodType) bootstrapArgs.get(0);
            MethodHandle invokeMethod = (MethodHandle) bootstrapArgs.get(1);

            implementedInterfaceSignature = new MethodSignature((JavaClassType) expr.getMethodSignature().getType(),
                    expr.getMethodSignature().getName(),
                    declMethodType.getParameterTypes(),
                    declMethodType.getReturnType());

            if (bootstrapArgs.size() < 3 || invokeMethod.isFieldRef()) {
                throw new TranslatorException("not supported dynamic invoke stmts");
            }
            lambdaCodes.append(getInputParams(declMethodType.getParameterTypes()))
                    .append(" {").append(NEW_LINE);

            lambdaCodes.append(
                    getMethodBody(invokeMethod, args, declMethodType))
                    .append(TAB).append(TAB).append("}");
            return lambdaCodes.toString();
        } catch (ClassCastException | TranslatorException e) {
            throw new TranslatorException(e.getMessage());
        }
    }

    private String getMethodBody(MethodHandle methodHandle, List<Immediate> args,
                                 MethodType methodType) {
        StringJoiner params = new StringJoiner(", ");

        for (Immediate arg : args) {
            arg.accept(valueVisitor);
            params.add(valueVisitor.toCode());
            valueVisitor.clear();
        }

        MethodSignature signature;
        if (methodHandle.getReferenceSignature() instanceof MethodSignature) {
            signature = (MethodSignature) methodHandle.getReferenceSignature();
        } else {
            throw new TranslatorException("not support dynamic invoke kind");
        }

        StringBuilder stmts = new StringBuilder();
        String caller = getCaller(methodHandle, args, methodType, stmts);
        dealwith(methodType, args, signature, params, stmts);

        stmts.append(TAB_INLINE);
        if (! (signature.getType() instanceof VoidType)) {
            stmts.append(TranslatorUtils.formatParamType(signature.getType())).append("tmp = ");
        }
        if (CallerKind.CONSTRUCTOR.equals(callerKind)) {
            stmts.append(String.format(NEW_OBJ,
                    TranslatorUtils.formatClassName(signature.getDeclClassType().getFullyQualifiedName()),
                    params)).append(";").append(NEW_LINE);
        } else {
            stmts.append(caller).append(signature.getName()).append("(")
                    .append(params).append(");").append(NEW_LINE);
        }

        memoryManageBeforeReturn(signature, stmts);
        if (!(methodType.getReturnType() instanceof VoidType)) {
            stmts.append(TAB_INLINE).append("return tmp;").append(NEW_LINE);
        }
        return stmts.toString();
    }

    private void dealwith(MethodType methodType, List<Immediate> args, MethodSignature signature, StringJoiner params,
                          StringBuilder stmts) {
        for (int j = CallerKind.INPUT.equals(callerKind) ? 1 : 0; j < methodType.getParameterTypes().size(); j++) {
            Type declParamterType = methodType.getParameterTypes().get(j);
            int paramIndex = args.size() + j;
            Type paramType = signature.getParameterType(paramIndex);
            if (paramType.equals(declParamterType)) {
                params.add("param" + j);
            } else {
                String requiredType = TranslatorUtils.formatType(paramType);
                if ((declParamterType instanceof PrimitiveType && !(paramType instanceof PrimitiveType))
                        || (!(declParamterType instanceof PrimitiveType) && paramType instanceof PrimitiveType)) {
                    throw new TranslatorException(String.format(
                            "ref method %s input param type doesn't match it's implemented interfaces %s",
                            signature, implementedInterfaceSignature
                    ));
                }
                stmts.append(String.format(OBJ_TRANS, requiredType, paramIndex, "param" + j));
                params.add("in" + paramIndex);
            }
        }
    }

    private void memoryManageBeforeReturn(MethodSignature signature, StringBuilder stmts) {
        if ((implementedInterfaceSignature.getType() instanceof PrimitiveType
                && signature.getType() instanceof ClassType)
                || (implementedInterfaceSignature.getType() instanceof ClassType
                && signature.getType() instanceof PrimitiveType)) {
            throw new TranslatorException(String.format(
                    "ref method %s return type doesn't match it's implemented interfaces %s",
                    signature, implementedInterfaceSignature
            ));
        }

        int refMethodRefCount = TranslatorContext.getRefCount(signature);
        int refCount = TranslatorContext.getRefCount(implementedInterfaceSignature);

        if (refMethodRefCount == 1 && refCount == 0) {
            stmts.append(String.format(UNKNOWN_PUT_REF, "tmp"));
        }

        if (refMethodRefCount == 0 && refCount == 1) {
            stmts.append(String.format(GET_REF, "tmp"));
        }
    }

    private String getCaller(MethodHandle methodHandle, List<Immediate> args, MethodType methodType,
                             StringBuilder stmts) {
        String caller = "";
        if (methodHandle.getKind().equals(REF_INVOKE_STATIC)) {
            ClassType callerClassType = methodHandle.getReferenceSignature().getDeclClassType();
            caller = declClassType.equals(callerClassType)
                    ? "" : TranslatorUtils.formatClassName(callerClassType.getFullyQualifiedName()) + "::";
            callerKind = CallerKind.CLASS;
        } else if (methodHandle.getKind().equals(REF_INVOKE_CONSTRUCTOR)) {
            callerKind = CallerKind.CONSTRUCTOR;
        } else if (args.isEmpty()) {
            if (!methodHandle.getReferenceSignature().getDeclClassType()
                    .equals(methodType.getParameterTypes().get(0))) {
                String requiredClass = TranslatorUtils.formatClassName(
                        methodHandle.getReferenceSignature().getDeclClassType().getFullyQualifiedName());
                stmts.append(String.format(OBJ_TRANS, requiredClass, 0, "param0"));
                caller = "in0->";
            } else {
                caller = "param0->";
            }
            callerKind = CallerKind.INPUT;
        } else {
            args.remove(0).accept(valueVisitor);
            caller = valueVisitor.toCode() + "->";
            valueVisitor.clear();
            callerKind = CallerKind.OBJ;
        }
        return caller;
    }

    private String getInputParams(List<Type> parameterTypes) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < parameterTypes.size(); i++) {
            joiner.add(TranslatorUtils.formatParamType(parameterTypes.get(i)) + "param" + i);
        }
        return "[&](" + joiner + ")";
    }

    /**
     * DynamicInvoke method caller kind
     */
    public enum CallerKind {
        CLASS("CLASS"),
        INPUT("INPUT"),
        OBJ("OBJ"),
        CONSTRUCTOR("CONSTRUCTOR");

        private final String valStr;

        CallerKind(String valStr) {
            this.valStr = valStr;
        }

        public String toString() {
            return this.valStr;
        }
    }
}
