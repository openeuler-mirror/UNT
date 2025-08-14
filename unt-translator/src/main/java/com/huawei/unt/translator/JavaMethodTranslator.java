/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.MemoryReleaseOptimizer;
import com.huawei.unt.optimizer.Optimizers;
import com.huawei.unt.translator.visitor.TranslatorStmtVisitor;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;
import com.huawei.unt.type.UDFType;

import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.VoidType;
import sootup.java.core.JavaSootMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * JavaMethodTranslator translate java method to cpp code string
 *
 * @since 2025-05-19
 */
public class JavaMethodTranslator {
    private JavaMethodTranslator() {}

    /**
     * Translate method to cpp code strings
     *
     * @param type UDFType
     * @param method method
     * @param isLambda isLambda
     * @return translated cpp code string
     */
    public static String translateMethod(UDFType type, JavaSootMethod method, boolean isLambda) {
        if (method.isAbstract()) {
            return printVirtual(method);
        }

        MethodContext methodContext = new MethodContext(method, type);
        StringBuilder bodyBuilder = new StringBuilder();

        boolean isStaticInit = methodContext.isStaticInit();
        boolean isInit = methodContext.isInit();
        boolean isIgnore = methodContext.isIgnore();

        if (isIgnore) {
            bodyBuilder.append(printIgnoredBody(method));
        } else {
            Optimizers.optimize(methodContext);

            int tabSize = isStaticInit ? 2 : 1;

            TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, tabSize, isStaticInit);
            TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);

            // add old var declare
            bodyBuilder.append(printReassignLocalsDeclare(methodContext));

            // add ret as return local
            if (methodContext.needRet()) {
                bodyBuilder.append(TAB)
                        .append(TranslatorUtils.formatParamType(methodContext.getJavaMethod().getReturnType()))
                        .append(" ret;").append(NEW_LINE);
            }

            // add tmpObj to free ignored return values
            if (methodContext.hasIgnoredValues()) {
                bodyBuilder.append(TranslatorContext.TMP_OBJ_DECLARE);
            }

            for (int i = 0; i < methodContext.getStmts().size(); i++) {
                Stmt stmt = methodContext.getStmts().get(i);

                // deal with label
                if (methodContext.containsLabel(i)) {
                    bodyBuilder.append(NEW_LINE)
                            .append(methodContext.getLabelString(i))
                            .append(":").append(NEW_LINE);
                }

                // deal with makeNull
                for (LValue refs : methodContext.getMakeNullVars(i)) {
                    refs.accept(valueVisitor);
                    bodyBuilder.append(String.format(TranslatorContext.MAKE_NULL, valueVisitor.toCode()));
                    valueVisitor.clear();
                }
                // deal with retStmts
                if (methodContext.hasRet(i)) {
                    methodContext.getRetValue(i).accept(valueVisitor);
                    bodyBuilder.append(String.format(TranslatorContext.RET_ASSIGN, valueVisitor.toCode()));
                    valueVisitor.clear();
                }

                StringBuilder beforeCodes = new StringBuilder();
                StringBuilder afterCodes = new StringBuilder();

                // deal with get refs
                Map<LValue, Integer> getVars = methodContext.getGetRefVars(i);
                for (LValue lValue : getVars.keySet()) {
                    lValue.accept(valueVisitor);
                    String code = String.format(TranslatorContext.GET_REF, valueVisitor.toCode());
                    valueVisitor.clear();
                    if (getVars.get(lValue).equals(MemoryReleaseOptimizer.BEFORE)) {
                        beforeCodes.append(code);
                    } else {
                        afterCodes.append(code);
                    }
                }

                // deal with reassign
                if (methodContext.isReassignStmt(i)) {
                    Stmt originalStmt = methodContext.getJavaMethod().getBody().getStmts().get(i);
                    Local local = ((Local) ((JAssignStmt) originalStmt).getLeftOp());
                    String oldAssignCode = String.format(TranslatorContext.OLD_VAR_ASSIGN, local.getName());
                    String oldPutCode = String.format(TranslatorContext.OLD_VAR_PUT, local.getName());
                    beforeCodes.append(oldAssignCode);
                    afterCodes.append(oldPutCode);
                }

                // only before the end of a circle or before the first stmt after the circle
                Map<LValue, Integer> circleFreeVars = methodContext.getCircleFreeVars(i);
                for (LValue lValue : circleFreeVars.keySet()) {
                    lValue.accept(valueVisitor);
                    beforeCodes.append(String.format(TranslatorContext.CIRCLE_PUT_REF, valueVisitor.toCode()));
                    valueVisitor.clear();
                }

                // deal with free
                if (i == methodContext.getFreeLabel()) {
                    beforeCodes.append(NEW_LINE)
                            .append("free:")
                            .append(NEW_LINE);
                }

                Map<LValue, Integer> unknownFreeVars = methodContext.getUnknownFreeVars(i);
                for (LValue lValue : unknownFreeVars.keySet()) {
                    lValue.accept(valueVisitor);
                    String code = String.format(TranslatorContext.UNKNOWN_PUT_REF, valueVisitor.toCode());
                    valueVisitor.clear();
                    if (unknownFreeVars.get(lValue).equals(MemoryReleaseOptimizer.BEFORE)) {
                        beforeCodes.append(code);
                    } else {
                        afterCodes.append(code);
                    }
                }

                bodyBuilder.append(beforeCodes);

                // deal with init, skip it
                if (isInit && stmt instanceof JInvokeStmt && ((JInvokeStmt) stmt).getInvokeExpr().isPresent()
                        && ((JInvokeStmt) stmt).getInvokeExpr().get() instanceof JSpecialInvokeExpr
                        && ((JSpecialInvokeExpr) ((JInvokeStmt) stmt).getInvokeExpr().get()).getBase()
                        .equals(methodContext.getThisLocal())
                        && TranslatorContext.INIT_FUNCTION_NAME.equals(
                        ((JInvokeStmt) stmt).getInvokeExpr().get().getMethodSignature().getName())) {
                    continue;
                }

                if (stmt instanceof JInvokeStmt && methodContext.isIgnoredValue(i)
                        && ((JInvokeStmt) stmt).getInvokeExpr().isPresent()) {
                    ((JInvokeStmt) stmt).getInvokeExpr().get().accept(valueVisitor);
                    bodyBuilder.append(String.format(TranslatorContext.TMP_OBJ_ASSIGN, valueVisitor.toCode()))
                            .append(TranslatorContext.TMP_OBJ_FREE);
                    valueVisitor.clear();
                } else {
                    stmt.accept(stmtVisitor);
                    bodyBuilder.append(stmtVisitor.toCode());
                    stmtVisitor.clear();
                }
                if (stmt.toString().equals("interfaceinvoke r7.<org.apache.flink.streaming.api.functions.source."
                        + "SourceFunction$SourceContext: void collect(java.lang.Object)>(r4)")
                    && method.toString().equals("<com.meituan.data.rt.metrics.function.MetricsGenerateSource: "
                        + "void run(org.apache.flink.streaming.api.functions.source.SourceFunction$SourceContext)>")) {
                    bodyBuilder.append(TAB).append("callback->process();").append(NEW_LINE);
                }
                bodyBuilder.append(afterCodes);
                // deal with gotoFree
                if (methodContext.hasGoto(i)) {
                    bodyBuilder.append(TranslatorContext.GOTO_FREE);
                }
                if (methodContext.needRet() && i == methodContext.getFreeLabel()) {
                    bodyBuilder.append(TranslatorContext.RETURN_RET);
                }
            }
        }

        String headAndParams;

        if (isStaticInit) {
            headAndParams = "";
        } else if (isInit) {
            headAndParams = printInitFunctionHead(methodContext);
        } else if (isIgnore) {
            headAndParams = isLambda ? type.printLambdaHeadAndParams(methodContext)
                    : TranslatorUtils.formatParamType(method.getReturnType())
                    + TranslatorUtils.formatClassName(method.getDeclClassType().getFullyQualifiedName())
                    + "::" + method.getName() + "("
                    + TranslatorUtils.methodParamsToString(methodContext.getJavaMethod())
                    + ")" + NEW_LINE + "{" + NEW_LINE;
        } else {
            headAndParams = isLambda ? type.printLambdaHeadAndParams(methodContext)
                    : type.printHeadAndParams(methodContext);
        }

        String methodString = isStaticInit ? "" : NEW_LINE;

        methodString += headAndParams;

        Set<ClassType> staticClasses = methodContext.getStaticClasses();
        StringBuilder staticInit = new StringBuilder();
        for (ClassType staticClass : staticClasses) {
            staticInit.append(TAB).append(TranslatorUtils.formatType(staticClass)).append("::")
                    .append("initStaticField").append(";").append(NEW_LINE);
        }

        methodString += staticInit.toString();

        if (!isIgnore) {
            methodString += TranslatorUtils.printLocals(methodContext);
        }

        methodString += bodyBuilder;

        if (!isStaticInit) {
            methodString += "}";
        }

        return methodString;
    }

    private static StringBuilder printReassignLocalsDeclare(MethodContext methodContext) {
        StringBuilder reassignLocalsBuilder = new StringBuilder();
        for (Local local : methodContext.getReassignLocals()) {
            reassignLocalsBuilder.append(String.format(TranslatorContext.OLD_VAR_DECLARE,
                    TranslatorUtils.formatParamType(local.getType()), local.getName()));
        }
        return reassignLocalsBuilder;
    }

    private static String printVirtual(JavaSootMethod method) {
        StringBuilder virtualBuilder = new StringBuilder(NEW_LINE)
                .append(TranslatorContext.TAB).append("virtual ")
                .append(TranslatorUtils.formatType(method.getReturnType())).append(" ")
                .append(method.getName()).append("(");

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            joiner.add(TranslatorUtils.formatParamType(method.getParameterType(i)) + " param" + i);
        }

        virtualBuilder.append(joiner).append(") = 0;");
        return virtualBuilder.toString();
    }

    private static String printIgnoredBody(JavaSootMethod method) {
        if (method.getReturnType() instanceof PrimitiveType.ByteType
                || method.getReturnType() instanceof PrimitiveType.ShortType
                || method.getReturnType() instanceof PrimitiveType.IntType
                || method.getReturnType() instanceof PrimitiveType.LongType
                || method.getReturnType() instanceof PrimitiveType.BooleanType) {
            return TAB + "return 0;" + NEW_LINE;
        } else if (method.getReturnType() instanceof PrimitiveType.CharType) {
            return TAB + "return ' ';" + NEW_LINE;
        } else if (method.getReturnType() instanceof VoidType) {
            return TAB + "return;" + NEW_LINE;
        } else if (method.getReturnType() instanceof PrimitiveType.FloatType
                || method.getReturnType() instanceof PrimitiveType.DoubleType) {
            return TAB + "return 0.0;" + NEW_LINE;
        } else {
            return TAB + "return nullptr;" + NEW_LINE;
        }
    }

    private static String printInitFunctionHead(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclaringClassType());
        StringBuilder initBuilder = new StringBuilder(className)
                .append("::")
                .append(className)
                .append("(")
                .append(TranslatorUtils.methodParamsToString(methodContext))
                .append(")");

        for (Stmt stmt : methodContext.getStmts()) {
            // deal with super class constructor
            if (stmt instanceof JInvokeStmt && ((JInvokeStmt) stmt).getInvokeExpr().isPresent()
                    && ((JInvokeStmt) stmt).getInvokeExpr().get() instanceof JSpecialInvokeExpr
                    && ((JSpecialInvokeExpr) ((JInvokeStmt) stmt).getInvokeExpr().get()).getBase()
                            .equals(methodContext.getThisLocal())
                    && TranslatorContext.INIT_FUNCTION_NAME.equals(
                            ((JInvokeStmt) stmt).getInvokeExpr().get().getMethodSignature().getName())) {
                MethodSignature signature = ((JInvokeStmt) stmt).getInvokeExpr().get().getMethodSignature();

                ClassType declClassType = ((JInvokeStmt) stmt).getInvokeExpr().get()
                        .getMethodSignature().getDeclClassType();
                List<Immediate> args = ((JInvokeStmt) stmt).getInvokeExpr().get().getArgs();

                if (signature.getParameterCount() > 0) {
                    initBuilder.append(" : ").append(TranslatorUtils.formatType(declClassType))
                            .append(TranslatorUtils.paramsToString(signature, args, methodContext));
                }
            }
        }

        initBuilder.append(NEW_LINE).append("{").append(NEW_LINE);

        return initBuilder.toString();
    }
}
