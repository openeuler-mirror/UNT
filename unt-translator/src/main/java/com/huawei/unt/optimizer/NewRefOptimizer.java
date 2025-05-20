package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo: fill it
public class NewRefOptimizer implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    /**
     * new has two type
     *
     * 1、has one new ref and one init function
     * 2、has one new  and one init function and two cast
     *
     * skip first init and two cast
     */
    @Override
    public void optimize(MethodContext methodContext) {
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getRightOp() instanceof JNewExpr) {
                Local newLocal = (Local) ((JAssignStmt) stmt).getLeftOp();
                boolean isInit = false;
                Set<Local> initLocals = new HashSet<>();
                initLocals.add(newLocal);

                if (!methodContext.getBeforeLocals(i).contains(newLocal)) {
                    methodContext.getStmts().set(i, Optimizers.getEmptyOptimizedStmt(stmt));
                    methodContext.addRemovedStmt(i);
                }

                for (int j = i + 1; j < stmts.size(); j++) {
                    // find init
                    Stmt stmtJ = stmts.get(j);
                    if (stmtJ instanceof JInvokeStmt
                            && ((JInvokeStmt) stmtJ).getInvokeExpr().isPresent()
                            && ((JInvokeStmt) stmtJ).getInvokeExpr().get() instanceof JSpecialInvokeExpr
                            && ((JInvokeStmt) stmtJ).getInvokeExpr().get().getMethodSignature().getName()
                                .equals(TranslatorContext.INIT_FUNCTION_NAME)
                            && initLocals.contains(((JSpecialInvokeExpr) (((JInvokeStmt) stmtJ).getInvokeExpr()
                                .get())).getBase())) {

                        if (TranslatorContext.IGNORED_CLASSES.contains(((JInvokeStmt) stmtJ).getInvokeExpr().get()
                                .getMethodSignature().getDeclClassType().getFullyQualifiedName())) {
                            methodContext.getStmts().set(j, Optimizers.getEmptyOptimizedStmt(stmtJ));
                            methodContext.addRemovedStmt(j);
                        } else {
                            methodContext.getStmts().set(j, new OptimizedLinesStmt(getInitFunction(newLocal,
                                    (JSpecialInvokeExpr) (((JInvokeStmt) stmtJ).getInvokeExpr().get()),
                                    methodContext), stmtJ));
                            methodContext.unRemoveStmt(i);
                        }
                        isInit = true;
                        continue;
                    }

                    // find no init cast and remove it
                    if (stmtJ instanceof JAssignStmt && ((JAssignStmt) stmtJ).getLeftOp() instanceof Local
                            && ((JAssignStmt) stmtJ).getRightOp() instanceof JCastExpr
                            && ((JCastExpr) ((JAssignStmt) stmtJ).getRightOp()).getOp() instanceof Local
                            && ((JCastExpr) ((JAssignStmt) stmtJ).getRightOp()).getOp().equals(newLocal)
                            && !isInit) {
                        initLocals.add((Local) ((JAssignStmt) stmtJ).getLeftOp());
                        methodContext.getStmts().set(j, Optimizers.getEmptyOptimizedStmt(stmtJ));
                        methodContext.addRemovedStmt(j);
                        continue;
                    }

                    // remove useless cast after init
                    if (stmtJ instanceof JAssignStmt && ((JAssignStmt) stmtJ).getLeftOp() instanceof Local
                            && ((JAssignStmt) stmtJ).getRightOp() instanceof JCastExpr
                            && ((JCastExpr) ((JAssignStmt) stmtJ).getRightOp()).getOp() instanceof Local
                            && initLocals.contains((Local) ((JCastExpr) ((JAssignStmt) stmtJ).getRightOp()).getOp())
                            && !methodContext.getAfterLocals(j).contains((Local) ((JAssignStmt) stmtJ).getLeftOp())
                            && isInit) {
                        methodContext.getStmts().set(j, Optimizers.getEmptyOptimizedStmt(stmtJ));
                        methodContext.addRemovedStmt(j);
                    }
                }
            }
        }
    }

    private static List<String> getInitFunction(Local newLocal, JSpecialInvokeExpr expr, MethodContext methodContext) {
        String initStmt = TranslatorUtils.formatLocalName(newLocal)
                 + " = new "
                 + TranslatorUtils.formatType(expr.getBase().getType())
                 + TranslatorUtils.paramsToString(expr.getMethodSignature(), expr.getArgs(), methodContext)
                 + ";";

        return ImmutableList.of(initStmt);
    }
}
