package com.huawei.unt.optimizer.stmts;

import com.google.common.base.Objects;
import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.visitor.ValueVisitor;
import sootup.core.types.Type;
import sootup.core.util.printer.StmtPrinter;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class OptimizedValue implements Value {

    private final String optimizedString;
    private final Value originalValue;

    public OptimizedValue(@Nonnull String optimizedString, @Nonnull Value optimizedValue) {
        this.optimizedString = optimizedString;
        this.originalValue = optimizedValue;
    }

    public String getCode() {
        return optimizedString;
    }

    public Value getOriginalValue() {
        return originalValue;
    }

    @Nonnull
    @Override
    public Stream<Value> getUses() {
        return originalValue.getUses();
    }

    @Nonnull
    @Override
    public Type getType() {
        return originalValue.getType();
    }

    @Override
    public void toString(@Nonnull StmtPrinter stmtPrinter) {
        stmtPrinter.literal(optimizedString);
    }

    @Override
    public int equivHashCode() {
        return Objects.hashCode(originalValue, optimizedString);
    }

    @Override
    public boolean equivTo(Object o, @Nonnull JimpleComparator jimpleComparator) {
        if (!(o instanceof OptimizedValue)) {
            return false;
        }

        OptimizedValue other = (OptimizedValue) o;
        return other.getCode().equals(this.getCode()) &&
                other.getOriginalValue().equivTo(this.getOriginalValue());
    }

    public void accept(@Nonnull ValueVisitor v) {
        v.defaultCaseValue(this);
    }
}
