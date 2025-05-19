package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.JavaMethodTranslator;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;
import com.huawei.unt.type.flink.FlinkReduceFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReduceTupleOptimizer implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return methodContext.getUdfType() instanceof FlinkReduceFunction &&
                methodContext.getJavaMethod().getReturnType() instanceof ClassType &&
                ((ClassType) methodContext.getJavaMethod().getReturnType()).getFullyQualifiedName()
                        .equals(Tuple2.class.getName());
    }

    @Override
    public void optimize(MethodContext methodContext) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        List<Stmt> stmts = methodContext.getJavaMethod().getBody().getStmts();

        Set<Local> returnLocals = new HashSet<>();
        Set<Local> paramLocals = new HashSet<>();
        Set<Local> fieldLocals = new HashSet<>();

        for (Stmt stmt : stmts) {
            if (stmt instanceof JReturnStmt && ((JReturnStmt) stmt).getOp() instanceof Local) {
                returnLocals.add((Local) ((JReturnStmt) stmt).getOp());
            }
            if (stmt instanceof JIdentityStmt && ((JIdentityStmt) stmt).getRightOp() instanceof JParameterRef) {
                paramLocals.add(((JIdentityStmt) stmt).getLeftOp());
            }
            if (stmt instanceof JAssignStmt &&
                    ((JAssignStmt) stmt).getLeftOp() instanceof Local &&
                    ((JAssignStmt) stmt).getRightOp() instanceof JInstanceFieldRef &&
                    paramLocals.contains(((JInstanceFieldRef) ((JAssignStmt) stmt).getRightOp()).getBase())) {
                fieldLocals.add((Local) ((JAssignStmt) stmt).getLeftOp());
            }
        }

        for (int i = 0; i < stmts.size(); i++) {
            Stmt stmt = stmts.get(i);
            if (stmt instanceof JAssignStmt &&
                    ((JAssignStmt) stmt).getLeftOp() instanceof Local &&
                    returnLocals.contains(((JAssignStmt) stmt).getLeftOp()) &&
                    ((JAssignStmt) stmt).getRightOp() instanceof JStaticInvokeExpr &&
                    ((JStaticInvokeExpr) ((JAssignStmt) stmt).getRightOp()).getMethodSignature().getDeclClassType()
                            .getFullyQualifiedName().equals(Tuple2.class.getName()) &&
                    ((JStaticInvokeExpr) ((JAssignStmt) stmt).getRightOp()).getMethodSignature().getName().equals("of")) {
                List<Immediate> args = ((JStaticInvokeExpr) ((JAssignStmt) stmt).getRightOp()).getArgs();
                boolean needChange = false;
                String leftValue;
                String rightValue;
                args.get(0).accept(valueVisitor);
                if (args.get(0) instanceof Local && fieldLocals.contains((Local) args.get(0))) {
                    needChange = true;
                    leftValue = valueVisitor.toCode() + "->clone()";
                } else {
                    leftValue = valueVisitor.toCode();
                }
                valueVisitor.clear();
                args.get(1).accept(valueVisitor);
                if (args.get(1) instanceof Local && fieldLocals.contains((Local) args.get(1))) {
                    needChange = true;
                    rightValue = valueVisitor.toCode() + "->clone()";
                } else {
                    rightValue = valueVisitor.toCode();
                }

                if (needChange && methodContext.getStmts().get(i) instanceof JAssignStmt) {
                    String chaneValue = "Tuple2::of(" + leftValue + ", " + rightValue + ")";
                    OptimizedValue value = new OptimizedValue(chaneValue, ((JAssignStmt) stmt).getRightOp());
                    methodContext.getStmts().set(i,
                            new OptimizedJAssignStmt(((JAssignStmt) stmt).getLeftOp(), value, (JAssignStmt) stmt));
                }
            }
        }
    }
}
