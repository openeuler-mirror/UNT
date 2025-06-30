/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import com.huawei.unt.model.MethodContext;
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
import sootup.core.types.Type;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * DynamicInvoke stmt translator
 *
 * @since 2025-06-30
 */
public class DynamicInvokeHandle implements Optimizer {
    private static final String NEW_OBJ = "new %s(%s)";
    private static final String TAB_INLINE = TAB + TAB + TAB;
    private static final String OBJ_TRANS = TAB_INLINE + "%1$s *in%2$d = (%1$s*) %3$s;" + NEW_LINE;

    private TranslatorValueVisitor valueVisitor;

    @Override
    public boolean fetch(MethodContext methodContext) {
        return !methodContext.getStmts().isEmpty();
    }

    @Override
    public void optimize(MethodContext methodContext) {
        valueVisitor = new TranslatorValueVisitor(methodContext);
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            Stmt stmt = methodContext.getStmts().get(i);
            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getRightOp() instanceof JDynamicInvokeExpr) {
                JDynamicInvokeExpr dynamicInvokeExpr = (JDynamicInvokeExpr) ((JAssignStmt) stmt).getRightOp();
                List<Immediate> args = dynamicInvokeExpr.getArgs().stream()
                        .filter(immediate -> !immediate.equals(methodContext.getThisLocal()))
                        .collect(Collectors.toList());
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

            if (bootstrapArgs.size() != 3 || invokeMethod.isFieldRef()) {
                throw new TranslatorException("not supported dynamic invoke stmts");
            }
            MethodSignature invokeMethodReferenceSignature = (MethodSignature) invokeMethod.getReferenceSignature();
            lambdaCodes.append(getInputParams(declMethodType.getParameterTypes()))
                    .append(" {").append(NEW_LINE);

            lambdaCodes.append(
                            getMethodBody(invokeMethodReferenceSignature, args, declMethodType.getParameterTypes()))
                    .append(TAB).append(TAB).append("}");
            return lambdaCodes.toString();
        } catch (ClassCastException e) {
            throw new TranslatorException(e.getMessage());
        }
    }

    private String getMethodBody(MethodSignature signature, List<Immediate> args,
                                 List<Type> declParameterTypes) {
        StringJoiner params = new StringJoiner(", ");
        StringBuilder stmts = new StringBuilder();

        for (Immediate arg : args) {
            arg.accept(valueVisitor);
            params.add(valueVisitor.toCode());
            valueVisitor.clear();
        }
        for (int j = 0; j < declParameterTypes.size(); j++) {
            Type declParamterType = declParameterTypes.get(j);
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
        stmts.append(TAB_INLINE).append(signature.getName()).append("(")
                .append(params).append(");").append(NEW_LINE);
        return stmts.toString();
    }

    private String getInputParams(List<Type> parameterTypes) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < parameterTypes.size(); i++) {
            joiner.add(TranslatorUtils.formatParamType(parameterTypes.get(i)) + "param" + i);
        }
        return "[&](" + joiner + ")";
    }
}
