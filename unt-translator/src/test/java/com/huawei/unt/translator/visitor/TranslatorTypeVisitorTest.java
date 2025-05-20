package com.huawei.unt.translator.visitor;

import com.huawei.unt.BaseTest;
import com.huawei.unt.translator.TranslatorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sootup.core.jimple.visitor.TypeVisitor;
import sootup.core.types.ArrayType;
import sootup.core.types.NullType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.UnknownType;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;

import javax.annotation.Nonnull;

public class TranslatorTypeVisitorTest extends BaseTest {
    @Test
    public void primaryTypeTest() {
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.BooleanType.getInstance()), "bool");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.DoubleType.getInstance()), "double");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.FloatType.getInstance()), "float");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.IntType.getInstance()), "int32_t");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.LongType.getInstance()), "int64_t");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.CharType.getInstance()), "char");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.ShortType.getInstance()), "int16_t");
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(PrimitiveType.ByteType.getInstance()), "int8_t");
    }

    @Test
    public void arrayTypeTest() {
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(new ArrayType(PrimitiveType.IntType.getInstance(), 10)), "Array");
    }

    @Test
    public void nullTypeTest() {
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(NullType.getInstance()), "null_ptr");
    }

    @Test
    public void voidTypeTest() {
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(VoidType.getInstance()), "void");
    }

    @Test
    public void classTypeTest() {
        Assertions.assertEquals(TranslatorTypeVisitor.getTypeString(JavaIdentifierFactory.getInstance().getType("java.lang.Integer")), "java_lang_Integer");
    }

    @Test
    public void unknownTypeTest() {
        Assertions.assertThrows(TranslatorException.class, () -> TranslatorTypeVisitor.getTypeString(UnknownType.getInstance()));
        Assertions.assertThrows(TranslatorException.class, () -> TranslatorTypeVisitor.getTypeString(new OtherType()));
    }

    private static class OtherType extends Type {
        @Override
        public <X extends TypeVisitor> TypeVisitor accept(@Nonnull X x) {
            x.defaultCaseType();
            return x;
        }
    }
}
