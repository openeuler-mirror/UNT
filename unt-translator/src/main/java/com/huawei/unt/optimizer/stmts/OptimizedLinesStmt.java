/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer.stmts;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;

import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * OptimizedLineStmt used for multi new Stmt create by optimizers
 *
 * @since 2025-05-19
 */
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

    @Override
    public <V extends StmtVisitor> V accept(@Nonnull V v) {
        v.defaultCaseStmt(this);
        return v;
    }
}
