//package com.huawei.unt.optimizer;
//
//import com.google.common.collect.ImmutableList;
//import com.huawei.unt.model.MethodContext;
//import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
//import com.huawei.unt.translator.visitor.TranslatorValueVisitor;
//import sootup.core.jimple.basic.Local;
//import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
//import sootup.core.jimple.common.stmt.JAssignStmt;
//import sootup.core.jimple.common.stmt.Stmt;
//import sootup.core.signatures.MethodSignature;
//
//import java.util.List;
//
//public class StringBuilderOptimizer implements Optimizer {
//
//    private static final String STRING_BUILDER_CLASS = StringBuilder.class.getName();
//    private static final String APPEND_METHOD_NAME = "append";
//
//    @Override
//    public boolean fetch(MethodContext methodContext) {
//        for (Stmt stmt : methodContext.getStmts()) {
//            if (isStringBuilderAppend(stmt)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public void optimize(MethodContext methodContext) {
//        List<Stmt> stmts = methodContext.getStmts();
//
//        for (int i = 0; i < stmts.size(); i++) {
//            if (isStringBuilderAppend(stmts.get(i))) {
//                JAssignStmt stmt = (JAssignStmt) methodContext.getStmts().get(i);
//                Local returnLocal = (Local) stmt.getLeftOp();
//                Local baseLocal = ((JVirtualInvokeExpr) stmt.getRightOp()).getBase();
//                methodContext.mergeLocals(returnLocal, baseLocal);
//
//                // replace stmt
//                TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
//                stmt.getRightOp().accept(valueVisitor);
//                methodContext.getStmts().set(i, new OptimizedLinesStmt(ImmutableList.of(valueVisitor.toCode() + ";"), stmt));
//            }
//        }
//    }
//
//    private boolean isStringBuilderAppend(Stmt stmt) {
//        if (stmt instanceof JAssignStmt) {
//            JAssignStmt assignStmt = (JAssignStmt) stmt;
//
//            if (assignStmt.getLeftOp() instanceof Local
//                    && assignStmt.getRightOp() instanceof JVirtualInvokeExpr) {
//                MethodSignature methodSignature = ((JVirtualInvokeExpr) assignStmt.getRightOp()).getMethodSignature();
//
//                return methodSignature.getDeclClassType().toString().equals(STRING_BUILDER_CLASS)
//                        && methodSignature.getName().equals(APPEND_METHOD_NAME);
//            }
//        }
//
//        return false;
//    }
//}
