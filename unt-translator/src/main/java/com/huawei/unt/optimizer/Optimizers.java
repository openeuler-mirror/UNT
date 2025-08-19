/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;

import com.google.common.collect.ImmutableList;

import sootup.core.jimple.common.stmt.Stmt;

import java.util.Collections;
import java.util.List;

/**
 * Handle all optimizers
 *
 * @since 2505-05-19
 */
public class Optimizers {
    /**
     * All optimizers
     */
    public static final List<Optimizer> OPTIMIZERS = ImmutableList.of(
            new StmtGraphAnalyzer(),
            new BranchStmtLabeler(),
            new NewRefOptimizer(),
            new RemoveTrap(),
            new RemoveIgnoreClass(),
            new MemoryReleaseOptimizer(),
            new StringPacking(),
            new DynamicInvokeHandle(),
            new ArrayFieldHandler(),
            new InitStaticReturnHandler());

    /**
     * Use all optimizers
     *
     * @param methodContext methodContext
     */
    public static void optimize(MethodContext methodContext) {
        for (Optimizer optimizer : OPTIMIZERS) {
            if (optimizer.fetch(methodContext)) {
                optimizer.optimize(methodContext);
            }
        }
    }

    /**
     * Return an empty stmt
     *
     * @param stmt originalStmt
     * @return empty stmt
     */
    public static Stmt getEmptyOptimizedStmt(Stmt stmt) {
        return new OptimizedLinesStmt(Collections.emptyList(), stmt);
    }
}
