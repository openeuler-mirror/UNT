package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.util.List;

public class RemoveIgnoreClass implements Optimizer {

    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        for (int i = 0; i < stmts.size(); i++) {

            if (stmts.get(i) instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) stmts.get(i);

                if (stmt.getLeftOp().getType() instanceof ClassType &&
                        TranslatorContext.IGNORED_CLASSES.contains(
                                ((ClassType) stmt.getLeftOp().getType()).getFullyQualifiedName())) {
                    methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                    continue;
                }

                if (stmt.getRightOp().getType() instanceof ClassType &&
                        TranslatorContext.IGNORED_CLASSES.contains(
                                ((ClassType) stmt.getRightOp().getType()).getFullyQualifiedName())) {
                    methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                    continue;
                }

                if (stmt.getRightOp() instanceof AbstractInvokeExpr &&
                        TranslatorContext.IGNORED_CLASSES.contains(((AbstractInvokeExpr) stmt.getRightOp())
                                .getMethodSignature().getDeclClassType().getFullyQualifiedName())) {
                    methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                    continue;
                }
            }
        }
    }
}
