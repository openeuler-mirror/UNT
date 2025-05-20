package com.huawei.unt.translator.visitor;

import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import sootup.core.jimple.visitor.AbstractTypeVisitor;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;

import javax.annotation.Nonnull;

public class TranslatorTypeVisitor extends AbstractTypeVisitor {
    private final StringBuilder typeBuilder = new StringBuilder();

    private static final TranslatorTypeVisitor INSTANCE = new TranslatorTypeVisitor();

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
        typeBuilder.append("int8_t");
    }

    @Override
    public void caseCharType() {
        typeBuilder.append("char");
    }

    @Override
    public void caseShortType() {
        typeBuilder.append("int16_t");
    }

    @Override
    public void caseIntType() {
        typeBuilder.append("int32_t");
    }

    @Override
    public void caseLongType() {
        typeBuilder.append("int64_t");
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
        typeBuilder.append("Array");
//        throw new DependencyException("Array type need special handle now.");
    }

    @Override
    public void caseClassType(@Nonnull ClassType classType) {
        typeBuilder.append(TranslatorUtils.formatClassName(classType.getFullyQualifiedName()));
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
        throw new TranslatorException("Has unsupported type");
    }

    private void clear() {
        typeBuilder.delete(0, typeBuilder.length());
    }

    private String toCode() {
        return typeBuilder.toString();
    }

    private TranslatorTypeVisitor() {}
}
