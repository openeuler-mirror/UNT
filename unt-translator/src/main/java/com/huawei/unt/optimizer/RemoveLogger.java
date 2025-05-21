package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;

import java.util.Set;

public class RemoveLogger implements Optimizer {
    private static final Set<String> LOG_NAMES = ImmutableSet.of("Logger", "LoggerFactory");

    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            Stmt stmt = methodContext.getStmts().get(i);

            if (stmt instanceof JAssignStmt &&
                    ((JAssignStmt) stmt).getLeftOp() instanceof Local &&
                    ((JAssignStmt) stmt).getLeftOp().getType() instanceof ClassType &&
                    LOG_NAMES.contains(((ClassType) ((JAssignStmt) stmt).getLeftOp().getType()).getClassName())) {
                methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                methodContext.addRemovedStmt(i);
                continue;
            }

            if (stmt instanceof JInvokeStmt && ((JInvokeStmt) stmt).getInvokeExpr().isPresent() &&
                    LOG_NAMES.contains(((JInvokeStmt) stmt).getInvokeExpr().get().getMethodSignature()
                            .getDeclClassType().getClassName())) {
                methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                methodContext.addRemovedStmt(i);
                continue;
            }

            if (stmt instanceof JAssignStmt &&
                    ((JAssignStmt) stmt).getLeftOp() instanceof JStaticFieldRef &&
                    ((JAssignStmt) stmt).getLeftOp().getType() instanceof ClassType &&
                    LOG_NAMES.contains(((ClassType) ((JAssignStmt) stmt).getLeftOp().getType()).getClassName())) {
                methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                methodContext.addRemovedStmt(i);
            }
        }
    }
}
