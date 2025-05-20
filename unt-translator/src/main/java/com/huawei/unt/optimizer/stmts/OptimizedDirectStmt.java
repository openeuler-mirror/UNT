package com.huawei.unt.optimizer.stmts;


import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

import javax.annotation.Nonnull;
import java.util.Objects;

/*
    The optimizer returns the result directly.
 */
public class OptimizedDirectStmt extends AbstractStmt {
    private final Stmt originalStmt;
    private final String optimizedRes;

    public OptimizedDirectStmt(String optimizedRes, Stmt originalStmt){
        super(originalStmt.getPositionInfo());
        this.originalStmt = originalStmt;
        this.optimizedRes = optimizedRes;
    }

    public String getOptimizedRes(){
        return optimizedRes;
    }

    public Stmt getOriginalStmt(){
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
        stmtPrinter.literal(optimizedRes);
    }

    @Override
    public int equivHashCode() {
        return Objects.hash(originalStmt,optimizedRes);
    }

    @Override
    public boolean equivTo(Object o, @Nonnull JimpleComparator jimpleComparator) {
        if (!(o instanceof OptimizedDirectStmt)){
            return false;
        }

        OptimizedDirectStmt other = (OptimizedDirectStmt) o;
        return jimpleComparator.caseStmt(originalStmt, other.originalStmt)
                && optimizedRes.equals(other.optimizedRes);
    }

    @Override
    public <X extends StmtVisitor> StmtVisitor accept(@Nonnull X v) {
        v.defaultCaseStmt(this);
        return v;
    }
}
