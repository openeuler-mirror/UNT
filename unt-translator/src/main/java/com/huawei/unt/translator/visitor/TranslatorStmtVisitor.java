package com.huawei.unt.translator.visitor;

import com.huawei.unt.optimizer.stmts.OptimizedDirectStmt;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.model.MethodContext;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.javabytecode.stmt.JEnterMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JExitMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import sootup.core.jimple.visitor.AbstractStmtVisitor;

import javax.annotation.Nonnull;
import java.util.List;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class TranslatorStmtVisitor extends AbstractStmtVisitor {
    protected final StringBuilder stmtBuilder = new StringBuilder();

    protected final MethodContext methodContext;
    private final boolean isStaticInit;

    private int tabSize = 1;

    public TranslatorStmtVisitor(MethodContext methodContext, int tabSize, boolean isStaticInit) {
        this.methodContext = methodContext;
        this.tabSize = tabSize;
        this.isStaticInit = isStaticInit;
    }

    @Override
    public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
        if (!stmt.getInvokeExpr().isPresent()) {
            return;
        }
        printTab();
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getInvokeExpr().get().accept(valueVisitor);
        stmtBuilder.append(valueVisitor.toCode()).append(";").append(NEW_LINE);
    }

    @Override
    public void caseIdentityStmt(@Nonnull JIdentityStmt stmt) {
        if (stmt.getRightOp() instanceof JParameterRef) {
            Local left = stmt.getLeftOp();
            JParameterRef param = (JParameterRef) stmt.getRightOp();
            methodContext.putParams(param.getIndex(), left);
        } else if (stmt.getRightOp() instanceof JThisRef) {
            // skip this local
            methodContext.removeLocal(stmt.getLeftOp());
//            methodContext.setThisLocal(stmt.getLeftOp());
        } else if (stmt.getRightOp() instanceof JCaughtExceptionRef) {
            // todo: skip it temp
        } else {
            defaultCaseStmt(stmt);
        }
    }

    @Override
    public void caseAssignStmt(@Nonnull JAssignStmt stmt) {
        printTab();

        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getLeftOp().accept(valueVisitor);
        String leftValue = valueVisitor.toCode();
        valueVisitor.clear();

        stmt.getRightOp().accept(valueVisitor);
        String rightValue = valueVisitor.toCode();

        stmtBuilder.append(leftValue).append(" = ").append(rightValue)
                .append(";").append(NEW_LINE);
    }

    @Override
    public void caseReturnStmt(@Nonnull JReturnStmt stmt) {
        stmtBuilder.append(NEW_LINE);

        printTab();
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getOp().accept(valueVisitor);
        stmtBuilder.append("return ").append(valueVisitor.toCode()).append(";").append(NEW_LINE);
    }

    @Override
    public void caseReturnVoidStmt(@Nonnull JReturnVoidStmt stmt) {
        stmtBuilder.append(NEW_LINE);
        printTab();
        stmtBuilder.append("return;").append(NEW_LINE);
    }

    // todo:
    @Override
    public void caseGotoStmt(@Nonnull JGotoStmt stmt) {
        printTab();
        stmtBuilder.append("goto ")
                .append(methodContext.getLabelString(methodContext.getBranchTargets(stmt).get(0)))
                .append(";")
                .append(NEW_LINE);
    }

    @Override
    public void caseIfStmt(@Nonnull JIfStmt stmt) {
        printTab();

        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getCondition().accept(valueVisitor);
        stmtBuilder.append("if ( ").append(valueVisitor.toCode()).append(" ) {").append(NEW_LINE);

        printTab();
        List<Integer> indexes = methodContext.getBranchTargets(stmt);

        stmtBuilder.append(TAB).append("goto ").append(methodContext.getLabelString(indexes.get(0)))
                .append(";").append(NEW_LINE);

        printTab();
        stmtBuilder.append("}").append(NEW_LINE);

    }

    @Override
    public void caseEnterMonitorStmt(@Nonnull JEnterMonitorStmt stmt) {
        printTab();
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getOp().accept(valueVisitor);
        stmtBuilder.append(valueVisitor.toCode()).append("->mutex.lock();").append(NEW_LINE);
    }

    @Override
    public void caseExitMonitorStmt(@Nonnull JExitMonitorStmt stmt) {
        printTab();
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getOp().accept(valueVisitor);
        stmtBuilder.append(valueVisitor.toCode()).append("->mutex.unlock();").append(NEW_LINE);
    }

    // todo:
    @Override
    public void caseSwitchStmt(@Nonnull JSwitchStmt stmt) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        stmt.getKey().accept(valueVisitor);
        String key = valueVisitor.toCode();

        stmt.getValues();

        List<Integer> targets = methodContext.getBranchTargets(stmt);

        printTab();
        stmtBuilder.append("switch (").append(key).append(") {").append(NEW_LINE);
        List<IntConstant> values = stmt.getValues();
        for (int i = 0; i < values.size(); i++) {
            printTab();
            stmtBuilder.append(TAB).append("case ").append(values.get(i).getValue()).append(":").append(NEW_LINE);
            printTab();
            stmtBuilder.append(TAB).append(TAB).append("goto ")
                    .append(methodContext.getLabelString(targets.get(i))).append(";").append(NEW_LINE);
        }

        printTab();
        stmtBuilder.append(TAB).append("default:").append(NEW_LINE);
        printTab();
        stmtBuilder.append(TAB).append(TAB).append("goto ")
                .append(methodContext.getLabelString(targets.get(targets.size() - 1))).append(";").append(NEW_LINE);
        printTab();
        stmtBuilder.append("}").append(NEW_LINE);
    }

    @Override
    public void caseThrowStmt(@Nonnull JThrowStmt stmt) {
        // do nothing now
    }

    @Override
    public void defaultCaseStmt(@Nonnull Stmt stmt) {
        if (stmt instanceof OptimizedLinesStmt) {
            for (String code : ((OptimizedLinesStmt) stmt).getOptimizedCodes()) {
                printTab();
                stmtBuilder.append(code).append(NEW_LINE);
            }
            return;
        }

        if (stmt instanceof OptimizedJAssignStmt) {
            printTab();

            TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
            ((OptimizedJAssignStmt) stmt).getLeftValue().accept(valueVisitor);
            String leftCode = valueVisitor.toCode();
            valueVisitor.clear();
            stmtBuilder.append(leftCode).append(" = ");
            ((OptimizedJAssignStmt) stmt).getRightValue().accept(valueVisitor);
            String rightCode = valueVisitor.toCode();
            stmtBuilder.append(rightCode).append(";").append(NEW_LINE);

            return;
        }

        if (stmt instanceof OptimizedDirectStmt){
            printTab();
            stmtBuilder.append(((OptimizedDirectStmt) stmt).getOptimizedRes()).append(";").append(NEW_LINE);
            return;
        }

        throw new TranslatorException("Can't translate stmt type: " + stmt.getClass().getSimpleName());
    }

    public void clear() {
        stmtBuilder.delete(0, stmtBuilder.length());
    }

    public void increaseTab() {
        tabSize++;
    }

    public void decreaseTab() {
        tabSize--;
    }

    private void printTab() {
        for (int i = 0; i < tabSize; i++) {
            stmtBuilder.append(TAB);
        }
    }

    public String toCode() {
        return stmtBuilder.toString();
    }
}
