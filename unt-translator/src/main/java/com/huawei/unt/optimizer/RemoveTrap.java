/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Trap;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.util.printer.BriefStmtPrinter;

import java.util.List;

/**
 * Remove try catch stmt
 *
 * @since 2025-05-22
 */
public class RemoveTrap implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        StmtGraph<?> graph = methodContext.getJavaMethod().getBody().getStmtGraph();
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        BriefStmtPrinter stmtPrinter = new BriefStmtPrinter();
        stmtPrinter.buildTraps(graph);
        Iterable<Trap> traps = stmtPrinter.getTraps();

        for (Trap trap : traps) {
            Stmt handleStmt = trap.getHandlerStmt();

            BasicBlock<?> block = graph.getBlockOf(handleStmt);

            for (Stmt stmt : block.getStmts()) {
                methodContext.getStmts().set(stmts.indexOf(stmt), Optimizers.getEmptyOptimizedStmt(stmt));
                methodContext.addRemovedStmt(stmts.indexOf(stmt));
            }
        }
    }
}
