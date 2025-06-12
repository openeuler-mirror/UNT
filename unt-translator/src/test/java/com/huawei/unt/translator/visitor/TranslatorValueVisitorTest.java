/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.BooleanConstant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.constant.EnumConstant;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.MethodType;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JNegExpr;
import sootup.core.jimple.common.expr.JUshrExpr;
import sootup.core.jimple.common.expr.JAndExpr;
import sootup.core.jimple.common.expr.JOrExpr;
import sootup.core.jimple.common.expr.JXorExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JShlExpr;
import sootup.core.jimple.common.expr.JShrExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.VoidType;
import sootup.core.types.PrimitiveType;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.FieldSignature;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test TranslatorValueVisitor
 *
 * @since 2025-05-19
 */
public class TranslatorValueVisitorTest extends BaseTest {
    private static MethodContext methodContext;
    private static final JavaView VIEW = new JavaView(PathBasedAnalysisInputLocation.create
        (Paths.get("src/test/resources/optimizer/binary"), SourceType.Application, INTERCEPTORS));
    private static final JavaIdentifierFactory IDFACTORY = JavaIdentifierFactory.getInstance();

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
            PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        JarHandler jarHandler = new JarHandler(javaView);
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestTrap", "checkException", "void", ImmutableList.of());
        methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
    }

    @Test
    public void testLocal() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        Local local = new Local("id", PrimitiveType.IntType.getInstance());
        local.accept(valueVisitor);
        assertEquals("id", valueVisitor.toCode());
    }

    @Test
    public void testBooleanConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        BooleanConstant trueConstant = BooleanConstant.getTrue();
        trueConstant.accept(valueVisitor);
        assertEquals("true", valueVisitor.toCode());
        valueVisitor.clear();
        BooleanConstant falseConstant = BooleanConstant.getFalse();
        falseConstant.accept(valueVisitor);
        assertEquals("false", valueVisitor.toCode());
    }

    @Test
    public void testDoubleConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        DoubleConstant doubleConstant = DoubleConstant.getInstance(101.5);
        doubleConstant.accept(valueVisitor);
        assertEquals(valueVisitor.toCode(), "101.5");
    }

    @Test
    public void testFloatConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        FloatConstant.getInstance(25.3f).accept(valueVisitor);
        assertEquals("25.3f", valueVisitor.toCode());
    }

    @Test
    public void testIntConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        IntConstant.getInstance(42).accept(valueVisitor);
        assertEquals("42", valueVisitor.toCode());
    }

    @Test
    public void testLongConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        LongConstant.getInstance(1000L).accept(valueVisitor);
        assertEquals("1000L", valueVisitor.toCode());
    }

    @Test
    public void testNullConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        NullConstant.getInstance().accept(valueVisitor);
        assertEquals("nullptr", valueVisitor.toCode());
    }

    @Test
    public void testStringConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType stringType = factory.getClassType("java.lang.String");
        StringConstant constant = new StringConstant("test", stringType);
        constant.accept(valueVisitor);
        assertEquals("\"test\"", valueVisitor.toCode());
    }

    @Test
    public void testEnumConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType colorType = factory.getClassType("com.example.Color");
        EnumConstant redConstant = new EnumConstant("RED", colorType);
        redConstant.accept(valueVisitor);
        assertEquals("RED", valueVisitor.toCode());
    }

    @Test
    public void testClassConstant() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType stringType = factory.getClassType("java.lang.String");
        ClassConstant constant = new ClassConstant("java/lang/String", stringType);
        constant.accept(valueVisitor);
        assertEquals(
                "ClassConstant::getInstance().get(\"java/lang/String\")",
                valueVisitor.toCode()
        );
    }

    @Test
    public void testArrayRef() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType stringType = factory.getClassType("java.lang.String");
        ArrayType stringArrayType = new ArrayType(stringType, 1);
        Local arrayLocal = new Local("arr", stringArrayType);
        JArrayRef arrayRef = new JArrayRef(arrayLocal, IntConstant.getInstance(2));
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        arrayRef.accept(valueVisitor);
        assertEquals("arr->get(2)", valueVisitor.toCode());
    }

    @Test
    public void testInstanceFieldRef() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType stringType = factory.getClassType("java.lang.String");
        FieldSignature fieldSig = factory.getFieldSignature(
                "length",
                stringType,
                PrimitiveType.getInt()
        );
        Local baseLocal = new Local("str", stringType);
        JInstanceFieldRef fieldRef = new JInstanceFieldRef(baseLocal, fieldSig);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        fieldRef.accept(valueVisitor);
        assertEquals("str->length", valueVisitor.toCode());
    }

    @Test
    public void testStaticFieldRef() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType systemType = factory.getClassType("java.lang.System");
        ClassType printStreamType = factory.getClassType("java.io.PrintStream");
        FieldSignature fieldSig = factory.getFieldSignature(
                "out",
                systemType,
                printStreamType
        );
        JStaticFieldRef fieldRef = new JStaticFieldRef(fieldSig);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        fieldRef.accept(valueVisitor);
        assertEquals("java_lang_System::out", valueVisitor.toCode());
    }

    @Test
    public void testAddExpr() {
        JAddExpr addExpr = new JAddExpr(
                IntConstant.getInstance(5),
                IntConstant.getInstance(3)
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        addExpr.accept(valueVisitor);
        assertEquals("5 + 3", valueVisitor.toCode());
    }

    @Test
    public void testEqExpr() {
        Local variable = new Local("a", PrimitiveType.getInt());
        IntConstant constant = IntConstant.getInstance(0);
        JEqExpr eqExpr = new JEqExpr(variable, constant);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        eqExpr.accept(valueVisitor);
        assertEquals("a == 0", valueVisitor.toCode());
    }

    @Test
    public void testVirtualInvokeExpr() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType stringType = factory.getClassType("java.lang.String");
        MethodSignature methodSig = factory.getMethodSignature(
                stringType,
                "length",
                PrimitiveType.getInt(),
                Collections.emptyList()
        );
        Local baseLocal = new Local("str", stringType);
        JVirtualInvokeExpr invokeExpr = new JVirtualInvokeExpr(
                baseLocal,
                methodSig,
                Collections.emptyList()
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        invokeExpr.accept(valueVisitor);
        assertEquals("str->length()", valueVisitor.toCode());
    }

    @Test
    public void testSpecialInvokeExpr() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType objectType = factory.getClassType("java.lang.Object");
        MethodSignature methodSig = factory.getMethodSignature(
                objectType,
                "<init>",
                VoidType.getInstance(),
                Collections.emptyList()
        );
        Local baseLocal = new Local("obj", objectType);
        JSpecialInvokeExpr invokeExpr = new JSpecialInvokeExpr(
                baseLocal,
                methodSig,
                Collections.emptyList()
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        invokeExpr.accept(valueVisitor);
        assertEquals("obj-><init>()", valueVisitor.toCode());
    }

    @Test
    public void testStaticInvokeExpr() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType mathType = factory.getClassType("java.lang.Math");
        MethodSignature methodSig = factory.getMethodSignature(
                mathType,
                "max",
                PrimitiveType.getInt(),
                ImmutableList.of(
                        PrimitiveType.getInt(),
                        PrimitiveType.getInt()
                )
        );
        List<Immediate> args = new ArrayList<>();
        args.add(IntConstant.getInstance(1));
        args.add(IntConstant.getInstance(2));

        JStaticInvokeExpr invokeExpr = new JStaticInvokeExpr(
                methodSig,
                args
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        invokeExpr.accept(valueVisitor);
        assertEquals("java_lang_Math::max(1, 2)", valueVisitor.toCode());
    }

    @Test
    public void testCastExprPrimitive() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JCastExpr castExpr = new JCastExpr(IntConstant.getInstance(100), PrimitiveType.getLong());
        castExpr.accept(valueVisitor);
        assertEquals("(int64_t) 100", valueVisitor.toCode());
    }

    @Test
    public void testInstanceOfExpr() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ClassType objectType = factory.getClassType("java.lang.Object");
        ClassType stringType = factory.getClassType("java.lang.String");
        Local obj = new Local("obj", objectType);
        JInstanceOfExpr instanceofExpr = new JInstanceOfExpr(obj, stringType);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        instanceofExpr.accept(valueVisitor);
        assertEquals("dynamic_cast<bool*>(obj) != nullptr", valueVisitor.toCode());
    }

    @Test
    public void testLengthExpr() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        ArrayType intArrayType = factory.getArrayType(PrimitiveType.getInt(), 1);
        Local arrayLocal = new Local("arr", intArrayType);
        JLengthExpr lengthExpr = new JLengthExpr(arrayLocal);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        lengthExpr.accept(valueVisitor);
        assertEquals("arr->size()", valueVisitor.toCode());
    }

    @Test
    public void testNegExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JNegExpr negExpr = new JNegExpr(IntConstant.getInstance(10));
        negExpr.accept(valueVisitor);
        assertEquals("- 10", valueVisitor.toCode());
    }

    @Test
    public void testUshrExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JUshrExpr ushrExpr = new JUshrExpr(IntConstant.getInstance(100), IntConstant.getInstance(2));
        ushrExpr.accept(valueVisitor);
        assertEquals("static_cast<int32_t>(static_cast<uint32_t>(100) >> 2)", valueVisitor.toCode());
    }

    @Test
    public void testAndExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JAndExpr expr = new JAndExpr(
                IntConstant.getInstance(0b1010),
                IntConstant.getInstance(0b1100)
        );
        expr.accept(valueVisitor);
        assertEquals("10 & 12", valueVisitor.toCode()); // 0b1010=10, 0b1100=12
    }

    @Test
    public void testOrExpr() {
        Local a = new Local("flags", PrimitiveType.getInt());
        IntConstant mask = IntConstant.getInstance(0x01);
        JOrExpr expr = new JOrExpr(a, mask);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("flags | 1", valueVisitor.toCode());
    }

    @Test
    public void testXorExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JXorExpr expr = new JXorExpr(
                LongConstant.getInstance(123L),
                LongConstant.getInstance(456L)
        );
        expr.accept(valueVisitor);
        assertEquals("123L ^ 456L", valueVisitor.toCode());
    }

    @Test
    public void testSubExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JSubExpr expr = new JSubExpr(
                DoubleConstant.getInstance(3.14),
                DoubleConstant.getInstance(1.59)
        );
        expr.accept(valueVisitor);
        assertEquals("3.14 - 1.59", valueVisitor.toCode());
    }

    @Test
    public void testMulExpr() {
        Local width = new Local("width", PrimitiveType.getInt());
        Local height = new Local("height", PrimitiveType.getInt());
        JMulExpr expr = new JMulExpr(width, height);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("width * height", valueVisitor.toCode());
    }

    @Test
    public void testDivExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JDivExpr expr = new JDivExpr(
                FloatConstant.getInstance(10.5f),
                FloatConstant.getInstance(2.0f)
        );
        expr.accept(valueVisitor);
        assertEquals("10.5f / 2.0f", valueVisitor.toCode());
    }

    @Test
    public void testRemExpr() {
        Local dividend = new Local("dividend", PrimitiveType.getInt());
        IntConstant divisor = IntConstant.getInstance(3);
        JRemExpr expr = new JRemExpr(dividend, divisor);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("dividend % 3", valueVisitor.toCode());
    }

    @Test
    public void testNeExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JNeExpr expr = new JNeExpr(
                new Local("status", PrimitiveType.getInt()),
                IntConstant.getInstance(0)
        );
        expr.accept(valueVisitor);
        assertEquals("status != 0", valueVisitor.toCode());
    }

    @Test
    public void testGeExpr() {
        Local score = new Local("score", PrimitiveType.getInt());
        IntConstant passing = IntConstant.getInstance(60);
        JGeExpr expr = new JGeExpr(score, passing);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("score >= 60", valueVisitor.toCode());
    }

    @Test
    public void testGtExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JGtExpr expr = new JGtExpr(
                DoubleConstant.getInstance(98.6),
                DoubleConstant.getInstance(37.0)
        );
        expr.accept(valueVisitor);
        assertEquals("98.6 > 37.0", valueVisitor.toCode());
    }

    @Test
    public void testLeExpr() {
        Local age = new Local("age", PrimitiveType.getInt());
        IntConstant limit = IntConstant.getInstance(18);
        JLeExpr expr = new JLeExpr(age, limit);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("age <= 18", valueVisitor.toCode());
    }

    @Test
    public void testLtExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JLtExpr expr = new JLtExpr(
                new Local("temperature", PrimitiveType.getFloat()),
                FloatConstant.getInstance(0.0f)
        );
        expr.accept(valueVisitor);
        assertEquals("temperature < 0.0f", valueVisitor.toCode());
    }

    @Test
    public void testCmpExpr() {
        // cmp 用于 long 类型比较
        JCmpExpr expr = new JCmpExpr(
                LongConstant.getInstance(100L),
                LongConstant.getInstance(200L)
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("100L > 200L ? 1 : (100L == 200L ? 0 : -1)", valueVisitor.toCode());
    }

    @Test
    public void testCmpgExpr() {
        JCmpgExpr expr = new JCmpgExpr(
                DoubleConstant.getInstance(1.0),
                DoubleConstant.getInstance(Double.NaN)
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("1.0 > NaN ? 1 : (1.0 == NaN ? 0 : -1)", valueVisitor.toCode());
    }

    @Test
    public void testCmplExpr() {
        JCmplExpr expr = new JCmplExpr(
                FloatConstant.getInstance(Float.NaN),
                FloatConstant.getInstance(1.0F)
        );
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("NaNf > 1.0f ? 1 : (NaNf == 1.0f ? 0 : -1)", valueVisitor.toCode());
    }

    @Test
    public void testShlExpr() {
        Local value = new Local("bits", PrimitiveType.getInt());
        IntConstant shift = IntConstant.getInstance(2);
        JShlExpr expr = new JShlExpr(value, shift);
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.accept(valueVisitor);
        assertEquals("bits << 2", valueVisitor.toCode());
    }

    @Test
    public void testShrExpr() {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        JShrExpr expr = new JShrExpr(
                LongConstant.getInstance(0xFFFFFFFFL),
                IntConstant.getInstance(16)
        );
        expr.accept(valueVisitor);
        assertEquals("4294967295L >> 16", valueVisitor.toCode()); // 0xFFFFFFFFL=4294967295
    }

    @Test
    public void testCaseNewExprThrows() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        ClassType objClassType = VIEW.getIdentifierFactory().getClassType("java.lang.Object");
        JNewExpr newExpr = new JNewExpr(objClassType);
        TranslatorException ex = assertThrows(
                TranslatorException.class,
                () -> newExpr.accept(visitor)
        );
        assertEquals("New expr should be special handle", ex.getMessage());
    }

    @Test
    public void testCaseNewArrayExpr() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        ClassType elementType = VIEW.getIdentifierFactory()
                .getClassType("java.lang.Object");
        ArrayType arrayType = new ArrayType(elementType, 1);
        IntConstant size = IntConstant.getInstance(3);
        JNewArrayExpr newArray = new JNewArrayExpr(arrayType, size, IDFACTORY);
        newArray.accept(visitor);
        assertEquals("new Array()", visitor.toCode());
    }

    @Test
    public void testCaseNewMultiArrayExprThrows() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        ClassType elem = VIEW.getIdentifierFactory().getClassType("java.lang.Object");
        ArrayType multiType = new ArrayType(elem, 2);
        IntConstant arg1 = IntConstant.getInstance(2);
        IntConstant arg2 = IntConstant.getInstance(4);
        List<Immediate> sizes = Arrays.asList(arg1, arg2);
        JNewMultiArrayExpr multi = new JNewMultiArrayExpr(multiType, sizes);
        TranslatorException ex = assertThrows(TranslatorException.class, () -> multi.accept(visitor));
        assertEquals("NewMultiArrayExpr is not supported now.", ex.getMessage());
    }

    @Test
    public void testCaseDynamicInvokeExprThrows() {
        MethodSignature bootstrapSig = VIEW.getIdentifierFactory()
                .getMethodSignature(
                        "java.lang.invoke.LambdaMetafactory",
                        "metafactory",
                        "java.lang.invoke.CallSite",
                        Arrays.asList(
                                "java.lang.invoke.MethodHandles$Lookup",
                                "java.lang.String",
                                "java.lang.invoke.MethodType",
                                "java.lang.invoke.MethodType",
                                "java.lang.invoke.MethodHandle",
                                "java.lang.invoke.MethodType"
                        )
                );
        ClassType dummy = VIEW.getIdentifierFactory()
                .getClassType(JDynamicInvokeExpr.INVOKEDYNAMIC_DUMMY_CLASS_NAME);
        MethodSignature invokeSig = VIEW.getIdentifierFactory()
                .getMethodSignature(dummy, "ignored", "java.lang.Object",
                        Arrays.asList("java.lang.Object"));
        ClassType stringType = IDFACTORY.getClassType("java.lang.String");
        List<Immediate> bsmArgs = Arrays.asList(new StringConstant("concat", stringType));
        List<Immediate> methodArgs = Arrays.asList(IntConstant.getInstance(42));
        JDynamicInvokeExpr dyn = new JDynamicInvokeExpr(
                bootstrapSig, bsmArgs,
                invokeSig, methodArgs
        );
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        TranslatorException ex = assertThrows(
                TranslatorException.class,
                () -> dyn.accept(visitor)
        );
        assertEquals("DynamicInvokeExpr is not supported yet", ex.getMessage());
    }

    @Test
    public void testDefaultCaseConstantViaDefaultCaseValue() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        LongConstant lc = LongConstant.getInstance(123L);
        TranslatorException ex = assertThrows(
                TranslatorException.class,
                () -> visitor.defaultCaseConstant(lc)
        );
        assertTrue(ex.getMessage().contains("Unsupported value"));
    }

    @Test
    public void testCaseCaughtExceptionRef() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        ClassType throwableType =
                VIEW.getIdentifierFactory().getClassType("java.lang.Throwable");
        JCaughtExceptionRef caught = new JCaughtExceptionRef(throwableType);
        caught.accept(visitor);
        assertEquals("*ex", visitor.toCode());
    }

    @Test
    public void testCaseThisRef() {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        ClassType cls = VIEW.getIdentifierFactory().getClassType("com.example.Dummy");
        JThisRef thisRef = new JThisRef(cls);
        thisRef.accept(visitor);
        assertEquals("this", visitor.toCode());
    }

    @Test
    public void testCaseMethodTypeThrows() {
        PrimitiveType retType = IDFACTORY.getPrimitiveType("int")
                .orElseThrow(() -> new RuntimeException("int type missing"));
        PrimitiveType paramType1 = IDFACTORY.getPrimitiveType("int")
                .orElseThrow(() -> new RuntimeException("int type missing"));
        ClassType objType = VIEW.getIdentifierFactory().getClassType("java.lang.Object");
        MethodSubSignature subSig = VIEW.getIdentifierFactory()
                .getMethodSubSignature(
                        "methodFoo",
                        retType,
                        Arrays.asList(paramType1, objType)
                );
        ClassType returnType = VIEW.getIdentifierFactory().getClassType("java.lang.String");
        MethodType mt = new MethodType(subSig, returnType);
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        TranslatorException ex = assertThrows(
                TranslatorException.class,
                () -> visitor.caseMethodType(mt)
        );
        assertEquals("MethodType is not supported yet", ex.getMessage());
    }
}