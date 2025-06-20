package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.translator.TranslatorContext;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.List;

/**
 * static init return an int constant (1), handle it
 */
public class InitStaticReturnHandler implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return TranslatorContext.STATIC_INIT_FUNCTION_NAME.equals(methodContext.getJavaMethod().getName());
    }

    @Override
    public void optimize(MethodContext methodContext) {
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof JReturnVoidStmt) {
                methodContext.getStmts().set(i, new OptimizedLinesStmt(ImmutableList.of("return 1;"), stmt));
            }
        }
    }
}
