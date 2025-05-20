/*
 * Copyright (C) 2025-2025. Huawei Technologies Co., Ltd. All rights reserved.
 */

package com.huawei.unt;

import sootup.core.jimple.visitor.AbstractTypeVisitor;
import sootup.core.types.ClassType;
import sootup.core.types.Type;

import javax.annotation.Nonnull;

public class TypePrinter extends AbstractTypeVisitor {
    protected final StringBuilder typeBuilder = new StringBuilder();

    private static final TypePrinter INSTANCE = new TypePrinter();

    public static String getTypeString(Type type) {
        type.accept(INSTANCE);
        String typeString = INSTANCE.toCode();
        INSTANCE.clear();
        return typeString;
    }

    @Override
    public void caseBooleanType() {
        typeBuilder.append("bool");
    }

    @Override
    public void caseByteType() {
        typeBuilder.append("byte");
    }

    @Override
    public void caseCharType() {
        typeBuilder.append("char");
    }

    @Override
    public void caseShortType() {
        typeBuilder.append("short");
    }

    @Override
    public void caseIntType() {
        typeBuilder.append("int");
    }

    @Override
    public void caseLongType() {
        typeBuilder.append("long");
    }

    @Override
    public void caseDoubleType() {
        typeBuilder.append("double");
    }

    @Override
    public void caseFloatType() {
        typeBuilder.append("float");
    }

    @Override
    public void caseArrayType() {
        typeBuilder.append("Array *");
    }

    @Override
    public void caseClassType(@Nonnull ClassType classType) {
        typeBuilder.append(classType.getFullyQualifiedName());
    }

    @Override
    public void caseNullType() {
        typeBuilder.append("null_ptr");
    }

    @Override
    public void caseVoidType() {
        typeBuilder.append("void");
    }

    @Override
    public void caseUnknownType() {
        this.defaultCaseType();
    }

    @Override
    public void defaultCaseType() {
        throw new UNTException("Has unsupported type");
    }

    public void clear() {
        typeBuilder.delete(0, typeBuilder.length());
    }

    public String toCode() {
        return typeBuilder.toString();
    }

    private TypePrinter() {}
}
