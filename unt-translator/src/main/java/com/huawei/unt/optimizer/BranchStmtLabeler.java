/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;

import sootup.core.jimple.common.stmt.BranchingStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deal with branch stmt
 *
 * @since 2025-05-19
 */
public class BranchStmtLabeler implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        Body body = methodContext.getJavaMethod().getBody();

        List<Stmt> stmts = body.getStmts();

        Map<Stmt, Integer> stmtIndex = new HashMap<>();

        Set<Integer> needLabels = new HashSet<>();

        for (int i = 0; i < stmts.size(); i++) {
            stmtIndex.put(stmts.get(i), i);
        }

        Map<Integer, List<Integer>> branches = new HashMap<>();

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof BranchingStmt) {
                List<Stmt> targetBranches = body.getStmtGraph().getBranchTargetsOf((BranchingStmt) stmt);
                List<Integer> targetList = new ArrayList<>();

                for (Stmt targetStmt : targetBranches) {
                    Integer index = stmtIndex.get(targetStmt);
                    needLabels.add(index);
                    targetList.add(index);
                }

                branches.put(i, targetList);
            }
        }

        methodContext.setLabels(needLabels.stream().sorted().collect(Collectors.toList()));
        methodContext.setBranches(branches);
    }
}
