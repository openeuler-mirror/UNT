package com.huawei.unt.optimizer;

import com.huawei.unt.model.JavaTrap;
import com.huawei.unt.model.MethodContext;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Trap;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.util.printer.BriefStmtPrinter;
import sootup.interceptors.LocalLivenessAnalyser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        for (int i = 0; i <stmts.size(); i++) {
            beforeLocals.put(i, localLivenessAnalyser.getLiveLocalsBeforeStmt(stmts.get(i)));
            afterLocals.put(i, localLivenessAnalyser.getLiveLocalsAfterStmt(stmts.get(i)));
        }

        methodContext.setBeforeLocals(beforeLocals);
        methodContext.setAfterLocals(afterLocals);

//        BriefStmtPrinter stmtPrinter = new BriefStmtPrinter();
//        stmtPrinter.buildTraps(graph);
//        Iterable<Trap> traps = stmtPrinter.getTraps();
//
//        List<JavaTrap> javaTraps = new ArrayList<>();
//        for (Trap trap : traps) {
//            javaTraps.add(new JavaTrap(
//                    trap.getExceptionType(),
//                    stmts.indexOf(trap.getBeginStmt()),
//                    stmts.indexOf(trap.getEndStmt()),
//                    stmts.indexOf(trap.getHandlerStmt())));
//
//        }
//        methodContext.setTraps(javaTraps);
    }
}
