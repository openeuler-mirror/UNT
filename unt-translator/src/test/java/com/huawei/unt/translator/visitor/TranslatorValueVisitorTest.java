/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
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
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JNegExpr;
import sootup.core.jimple.common.expr.JUshrExpr;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.model.SourceType;
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

/**
 * Test TranslatorValueVisitor
 *
 * @since 2025-05-19
 */
public class TranslatorValueVisitorTest extends BaseTest {
    private static MethodContext methodContext;

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
}
