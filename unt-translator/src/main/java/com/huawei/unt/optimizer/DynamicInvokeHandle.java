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

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * DynamicInvoke stmt translator
 *
 * @since 2025-06-30
 */
public class DynamicInvokeHandle implements Optimizer{
    private static final String TAB_INLINE = TAB + TAB + TAB;
    private static final String OBJ_TRANS = TAB_INLINE+ "%1$s *in%2$d = (%1$s*) %3$s;" + NEW_LINE;
    private static final String UNKNOWN_PUT_REF = TAB_INLINE + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB_INLINE + TAB + "%1$s->putRefCount();" + NEW_LINE
            + TAB_INLINE + "}" + NEW_LINE;
    private static final String GET_REF = TAB_INLINE + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB_INLINE + TAB + "%1$s->getRefCount();" + NEW_LINE + TAB + "}" + NEW_LINE;
    private ClassType declClassType;
    private TranslatorValueVisitor valueVisitor;
    private CallerKind callerKind;

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
                                    getLambdaCodes(dynamicInvokeExpr.getBootstrapArgs(), args)),
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

    private String getLambdaCodes(List<Immediate> bootstrapArgs, List<Immediate> args) {
        StringBuilder lambdaCodes = new StringBuilder();
        try {
            MethodType declMethodType = (MethodType) bootstrapArgs.get(0);
            MethodHandle invokeMethod = (MethodHandle) bootstrapArgs.get(1);

            if (bootstrapArgs.size() < 3 || invokeMethod.isFieldRef()) {
                throw new TranslatorException("not supported dynamic invoke stmts");
            }
            lambdaCodes.append(getInputParams(declMethodType.getParameterTypes()))
                    .append(" {").append(NEW_LINE);

            lambdaCodes.append(
                    getMethodBody(invokeMethod, args, declMethodType))
                    .append(TAB).append(TAB).append("}");
            return lambdaCodes.toString();
        } catch (ClassCastException e) {
            throw new TranslatorException(e.getMessage());
        }
    }

    private String getMethodBody(MethodHandle methodHandle, List<Immediate> args,
                                 MethodType methodType) {
        StringJoiner params = new StringJoiner(", ");
        StringBuilder stmts = new StringBuilder();
        String caller = getCaller(methodHandle, args, methodType, stmts);

        for (Immediate arg : args) {
            arg.accept(valueVisitor);
            params.add(valueVisitor.toCode());
            valueVisitor.clear();
        }

        MethodSignature signature = (MethodSignature) methodHandle.getReferenceSignature();
        for (int j = callerKind.equals(CallerKind.INPUT) ? 1 : 0; j < methodType.getParameterTypes().size(); j++) {
            Type declParamterType = methodType.getParameterTypes().get(j);
            int paramIndex = args.size() + j;
            Type paramType = signature.getParameterType(paramIndex);
            if (paramType.equals(declParamterType)) {
                params.add("param" + j);
            } else {
                String requiredType = TranslatorUtils.formatType(paramType);
                stmts.append(String.format(OBJ_TRANS, requiredType, paramIndex, "param" + j));
                params.add("in" + paramIndex);
            }
        }

        stmts.append(TAB_INLINE);
        if (! (methodType.getReturnType() instanceof VoidType)) {
            stmts.append("return ");
        }
        if (! (signature.getType() instanceof VoidType)) {
            stmts.append(TranslatorUtils.formatParamType(signature.getType())).append("tmp = ");
        }
        if (callerKind.equals(CallerKind.CONSTRUCTOR)) {
            stmts.append(String.format(NEW_OBJ,
                    TranslatorUtils.formatClassName(signature.getDeclClassType().getFullyQualifiedName()),
                    params)).append(";").append(NEW_LINE);
        } else {
            stmts.append(caller).append(signature.getName()).append("(")
                    .append(params).append(");").append(NEW_LINE);
        }

        memoryManageBeforeReturn(methodType, signature, stmts);
        if (!(methodType.getReturnType() instanceof VoidType)) {
            stmts.append(TAB_INLINE).append("return tmp;").append(NEW_LINE);
        }
        return stmts.toString();
    }

    private void memoryManageBeforeReturn(MethodType methodType, MethodSignature signature, StringBuilder stmts) {
        int refCount = TranslatorContext.getRefCount(signature);

        if (refCount == 1 && methodType.getReturnType() instanceof VoidType) {
            stmts.append(String.format(UNKNOWN_PUT_REF, "tmp"));
        }

        if (refCount == 0 && !(methodType.getReturnType() instanceof VoidType || methodType.getReturnType() instanceof PrimitiveType)) {
            stmts.append(String.format(GET_REF, "tmp"));
        }
    }

    private String getCaller(MethodHandle methodHandle, List<Immediate> args, MethodType methodType, StringBuilder stmts) {
        String caller = "";
        if (methodHandle.getKind().equals(REF_INVOKE_STATIC)) {
            ClassType callerClassType = methodHandle.getReferenceSignature().getDeclClassType();
            caller = callerClassType.equals(declClassType) ?
                    "" : TranslatorUtils.formatClassName(callerClassType.getFullyQualifiedName()) + "::";
            callerKind = CallerKind.CLASS;
        } else if (methodHandle.getKind().equals(REF_INVOKE_CONSTRUCTOR)) {
            callerKind = CallerKind.CONSTRUCTOR;
        } else if (args.isEmpty()){
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

    public enum CallerKind {
        CLASS(1, "CLASS"),
        INPUT(2, "INPUT"),
        OBJ(3, "OBJ"),
        CONSTRUCTOR(4, "CONSTRUCTOR");

        private final int val;
        private final String valStr;

        private CallerKind(int val, String valStr) {
            this.val = val;
            this.valStr = valStr;
        }

        public String toString() {
            return this.valStr;
        }

        public int getValue() {
            return this.val;
        }

        public String getValueName() {
            return this.valStr;
        }

    }
}
