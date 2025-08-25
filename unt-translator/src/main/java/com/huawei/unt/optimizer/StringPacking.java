/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedDirectStmt;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;

import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;

import java.util.List;

/**
 * StringPacking use for packing std::string to String object
 *
 * @since 2025-05-19
 */
public class StringPacking implements Optimizer {
    private static final ClassType STRING_CLASS_TYPE =
            JavaIdentifierFactory.getInstance().getClassType(String.class.getName());
    private static final ClassType OBJECT_CLASS_TYPE =
            JavaIdentifierFactory.getInstance().getClassType(Object.class.getName());

    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof JReturnStmt
                    && methodContext.getStmts().get(i) instanceof JReturnStmt
                    && ((JReturnStmt) stmt).getOp() instanceof StringConstant){
                String res = "new String(" + ((JReturnStmt) stmt).getOp().toString() + ")";
                OptimizedDirectStmt optimizedDirectStmt = new OptimizedDirectStmt("return " + res, stmt);
                methodContext.getStmts().set(i, optimizedDirectStmt);
            }
            if (stmt instanceof JAssignStmt
                    && (STRING_CLASS_TYPE.equals(((JAssignStmt) stmt).getLeftOp().getType())
                    || OBJECT_CLASS_TYPE.equals(((JAssignStmt) stmt).getLeftOp().getType()))
                    && ((((JAssignStmt) stmt).getRightOp() instanceof StringConstant)
                    || (((JAssignStmt) stmt).getRightOp() instanceof JVirtualInvokeExpr
                    && isToStringFunction((JVirtualInvokeExpr) ((JAssignStmt) stmt).getRightOp())))) {
                ((JAssignStmt) stmt).getRightOp().accept(valueVisitor);
                String stringCode = valueVisitor.toCode();
                valueVisitor.clear();
                ((JAssignStmt) stmt).getLeftOp().accept(valueVisitor);
                valueVisitor.clear();
                String rightCode = "new String(" + stringCode + ")";
                OptimizedValue rightValue = new OptimizedValue(rightCode, ((JAssignStmt) stmt).getLeftOp());
                Stmt needChangeStmt = methodContext.getStmts().get(i);
                if (needChangeStmt instanceof JAssignStmt) {
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(((JAssignStmt) stmt).getLeftOp(), rightValue, (JAssignStmt) stmt));
                }
                if (needChangeStmt instanceof OptimizedJAssignStmt) {
                    OptimizedJAssignStmt optStmt = (OptimizedJAssignStmt) needChangeStmt;
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(optStmt.getLeftValue(), rightValue, (JAssignStmt) stmt));
                }
            }
        }
    }

    private static boolean isToStringFunction(JVirtualInvokeExpr expr) {
        return expr.getArgCount() == 0 && expr.getMethodSignature().getName().equals("toString");
    }
}
