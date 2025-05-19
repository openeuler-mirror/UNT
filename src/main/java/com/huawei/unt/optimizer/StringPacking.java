package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractValueVisitor;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

            if (stmt instanceof JAssignStmt &&
                    (((JAssignStmt) stmt).getLeftOp().getType().equals(STRING_CLASS_TYPE) ||
                            ((JAssignStmt) stmt).getLeftOp().getType().equals(OBJECT_CLASS_TYPE)) &&
                    ((((JAssignStmt) stmt).getRightOp() instanceof StringConstant) ||
                            (((JAssignStmt) stmt).getRightOp() instanceof JVirtualInvokeExpr &&
                                    isToStringFunction((JVirtualInvokeExpr) ((JAssignStmt) stmt).getRightOp())))) {
                ((JAssignStmt) stmt).getRightOp().accept(valueVisitor);
                String stringCode = valueVisitor.toCode();
                valueVisitor.clear();

                ((JAssignStmt) stmt).getLeftOp().accept(valueVisitor);
                String leftCode = valueVisitor.toCode();
                valueVisitor.clear();


                String rightCode = "new String(" + stringCode + ")";

                OptimizedValue rightValue = new OptimizedValue(rightCode, ((JAssignStmt) stmt).getLeftOp());

                Stmt needChangeStmt = methodContext.getStmts().get(i);
                if (needChangeStmt instanceof JAssignStmt) {
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(((JAssignStmt) stmt).getLeftOp(), rightValue, (JAssignStmt) stmt));
                } else if (needChangeStmt instanceof OptimizedJAssignStmt) {
                    OptimizedJAssignStmt optStmt = (OptimizedJAssignStmt) needChangeStmt;
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(optStmt.getLeftValue(), rightValue, (JAssignStmt) stmt));
                }
//                else {
//                    String codeLine = leftCode + " = " + rightCode + ";";
//                    methodContext.getStmts().set(i, new OptimizedLinesStmt(ImmutableList.of(codeLine), stmt));
//                }
            }
        }
    }

    private static boolean isToStringFunction(JVirtualInvokeExpr expr) {
        return expr.getArgCount() == 0 && expr.getMethodSignature().getName().equals("toString");
    }
}
