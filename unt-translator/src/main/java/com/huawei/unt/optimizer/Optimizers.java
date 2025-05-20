package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.translator.TranslatorContext;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.Collections;
import java.util.List;

public class Optimizers {
    public static List<Optimizer> OPTIMIZERS = ImmutableList.of(
            new StmtGraphAnalyzer(),
            new BranchStmtLabeler(),
//            new ReduceTupleOptimizer(),
            new NewRefOptimizer(),
//            new RemoveLogger(),
            new RemoveTrap(),
            new RemoveIgnoreClass(),
            new MemoryReleaseOptimizer(TranslatorContext.LIB_INTERFACE_REF),
//            new StringBuilderOptimizer(),
            new ArrayFieldHandler(),
            new StringPacking(),
            new InitStaticReturnHandler());

    public static void optimize(MethodContext methodContext) {
        for (Optimizer optimizer : OPTIMIZERS) {
            if (optimizer.fetch(methodContext)) {
                optimizer.optimize(methodContext);
            }
        }
    }

    public static Stmt getEmptyOptimizedStmt(Stmt stmt) {
        return new OptimizedLinesStmt(Collections.emptyList(), stmt);
    }
}
