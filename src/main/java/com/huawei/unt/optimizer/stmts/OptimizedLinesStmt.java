package com.huawei.unt.optimizer.stmts;

import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;


public class OptimizedLinesStmt extends AbstractStmt {

    private final Stmt originalStmt;
    private final List<String> optimizedCodes;

    public OptimizedLinesStmt(@Nonnull List<String> optimizedCodes, @Nonnull Stmt originalStmt) {
        super(originalStmt.getPositionInfo());
        this.originalStmt = originalStmt;
        this.optimizedCodes = optimizedCodes;
    }

    public List<String> getOptimizedCodes() {
        return optimizedCodes;
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
        optimizedCodes.forEach(code -> stmtPrinter.literal(code + NEW_LINE));
    }

    @Override
    public int equivHashCode() {
        return Objects.hash(originalStmt, optimizedCodes);
    }

    @Override
    public boolean equivTo(Object o, @Nonnull JimpleComparator jimpleComparator) {
        if (!(o instanceof OptimizedLinesStmt)) {
            return false;
        }

        OptimizedLinesStmt other = (OptimizedLinesStmt) o;
        return jimpleComparator.caseStmt(originalStmt, other.originalStmt)
                && optimizedCodes.equals(other.optimizedCodes);
    }

    public <V extends StmtVisitor> V accept(@Nonnull V v) {
        v.defaultCaseStmt(this);
        return v;
    }
}
