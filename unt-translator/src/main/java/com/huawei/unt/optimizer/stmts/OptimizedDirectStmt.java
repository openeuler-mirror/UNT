/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer.stmts;

import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * The optimizer returns the result directly.
 *
 * @since 2025-05-19
 */
public class OptimizedDirectStmt extends AbstractStmt {
    private final Stmt originalStmt;
    private final String optimizedRes;

    public OptimizedDirectStmt(String optimizedRes, Stmt originalStmt) {
        super(originalStmt.getPositionInfo());
        this.originalStmt = originalStmt;
        this.optimizedRes = optimizedRes;
    }

    public String getOptimizedRes() {
        return optimizedRes;
    }

    public Stmt getOriginalStmt() {
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
        return Objects.hash(originalStmt, optimizedRes);
    }

    @Override
    public boolean equivTo(Object o, @Nonnull JimpleComparator jimpleComparator) {
        if (!(o instanceof OptimizedDirectStmt)) {
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
