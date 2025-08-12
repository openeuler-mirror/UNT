/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;

import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.interceptors.LocalLivenessAnalyser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyze stmt graph
 *
 * @since 2025-05-19
 */
public class StmtGraphAnalyzer implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        // all method need analyze
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        StmtGraph<?> graph = methodContext.getJavaMethod().getBody().getStmtGraph();
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        // analyzeLiveLocal
        LocalLivenessAnalyser localLivenessAnalyser = new LocalLivenessAnalyser(graph);
        Map<Integer, Set<Local>> beforeLocals = new HashMap<>();
        Map<Integer, Set<Local>> afterLocals = new HashMap<>();

        for (int i = 0; i < stmts.size(); i++) {
            beforeLocals.put(i, localLivenessAnalyser.getLiveLocalsBeforeStmt(stmts.get(i)));
            afterLocals.put(i, localLivenessAnalyser.getLiveLocalsAfterStmt(stmts.get(i)));
        }

        methodContext.setBeforeLocals(beforeLocals);
        methodContext.setAfterLocals(afterLocals);
    }
}
