/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import sootup.core.jimple.visitor.AbstractTypeVisitor;
import sootup.core.types.ClassType;
import sootup.core.types.Type;

import javax.annotation.Nonnull;

/**
 * Print type
 *
 * @since 2025-05-19
 */
public class TypePrinter extends AbstractTypeVisitor {
    private static final TypePrinter INSTANCE = new TypePrinter();

    private final StringBuilder typeBuilder = new StringBuilder();

    private TypePrinter() {}

    /**
     * Get Type code string
     *
     * @param type type
     * @return type code string
     */
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
        typeBuilder.append("nullptr");
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

    /**
     * clear type builder
     */
    public void clear() {
        typeBuilder.delete(0, typeBuilder.length());
    }

    /**
     * type to code string
     *
     * @return code string
     */
    public String toCode() {
        return typeBuilder.toString();
    }
}
