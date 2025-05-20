package com.huawei.unt.optimizer.stmts;

import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

import javax.annotation.Nonnull;
import java.util.Objects;

public class OptimizedJAssignStmt extends AbstractStmt {

    private final Value leftValue;
    private final Value rightValue;
    public final JAssignStmt originalStmt;

    public OptimizedJAssignStmt(@Nonnull Value leftValue, @Nonnull Value rightValue, @Nonnull JAssignStmt originalStmt) {
        super(originalStmt.getPositionInfo());
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.originalStmt = originalStmt;
    }

    public Value getLeftValue() {
        return leftValue;
    }

    public Value getRightValue() {
        return rightValue;
    }

    public JAssignStmt getOriginalStmt() {
        return originalStmt;
    }

    @Override
    public boolean fallsThrough() {
        return originalStmt.fallsThrough();
    }

    @Override
    public boolean branches() {
        return originalStmt.branches();
    }

    @Override
    public void toString(@Nonnull StmtPrinter stmtPrinter) {
        stmtPrinter.literal(leftValue.toString());
        stmtPrinter.literal(" = ");
        stmtPrinter.literal(rightValue.toString());
    }

    @Override
    public int equivHashCode() {
        return Objects.hash(originalStmt, leftValue, rightValue);
    }

    @Override
    public boolean equivTo(Object o, @Nonnull JimpleComparator jimpleComparator) {
        if (!(o instanceof OptimizedJAssignStmt)) {
            return false;
        }

        OptimizedJAssignStmt other = (OptimizedJAssignStmt) o;
        return Objects.equals(this.leftValue, other.leftValue) &&
                Objects.equals(this.rightValue, other.rightValue) &&
                jimpleComparator.caseAssignStmt(this.originalStmt, other.originalStmt);
    }

    public <V extends StmtVisitor> V accept(@Nonnull V v) {
        v.defaultCaseStmt(this);
        return v;
    }
}
