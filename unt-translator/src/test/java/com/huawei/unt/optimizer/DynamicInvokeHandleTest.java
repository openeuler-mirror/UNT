/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.visitor.TranslatorStmtVisitor;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Test DynamicInvokeHandle optimizer
 *
 * @since 2025-08-16
 */
class DynamicInvokeHandleTest extends BaseTest {
    private static final DynamicInvokeHandle DYNAMIC_INVOKE_HANDLE = new DynamicInvokeHandle();
    private static final String NEW_LINE = System.lineSeparator();
    private static JarHandler jarHandler;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        jarHandler = new JarHandler(javaView);
    }

    @Test
    public void testSimpleLambda() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestDynamicInvokeHandle", "testSimpleLambda", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());

        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        Assertions.assertTrue(DYNAMIC_INVOKE_HANDLE.fetch(methodContext));

        DYNAMIC_INVOKE_HANDLE.optimize(methodContext);

        Stmt dynamicStmt = methodContext.getStmts().get(5);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);
        dynamicStmt.accept(stmtVisitor);
        String actualRes = stmtVisitor.toCode();

        String expectedRes = "r5 = new java_util_function_Predicate([&](java_lang_Object *param0) {" + NEW_LINE
                + "            java_lang_String *in0 = (java_lang_String*) param0;" + NEW_LINE
                + "            bool tmp = lambda$testSimpleLambda$0(in0);" + NEW_LINE
                + "            return tmp;" + NEW_LINE
                + "        });" + NEW_LINE;
        Assertions.assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testRefContextObjLambda() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestDynamicInvokeHandle", "testRefContextObjLambda", "void", ImmutableList.of("java.lang.String"));
        Assertions.assertTrue(method.isPresent());

        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        Assertions.assertTrue(DYNAMIC_INVOKE_HANDLE.fetch(methodContext));

        DYNAMIC_INVOKE_HANDLE.optimize(methodContext);

        Stmt dynamicStmt = methodContext.getStmts().get(6);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);
        dynamicStmt.accept(stmtVisitor);
        String actualRes = stmtVisitor.toCode();

        String expectedRes = "r6 = new java_util_function_Predicate([&](java_lang_Object *param0) {" + NEW_LINE
                + "            java_lang_String *in1 = (java_lang_String*) param0;" + NEW_LINE
                + "            bool tmp = lambda$testRefContextObjLambda$1(r2, in1);" + NEW_LINE
                + "            return tmp;" + NEW_LINE
                + "        });" + NEW_LINE;
        Assertions.assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testStaticMethodRef() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestDynamicInvokeHandle", "testStaticMethodRef", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());

        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        Assertions.assertTrue(DYNAMIC_INVOKE_HANDLE.fetch(methodContext));

        DYNAMIC_INVOKE_HANDLE.optimize(methodContext);

        Stmt dynamicStmt = methodContext.getStmts().get(5);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);
        dynamicStmt.accept(stmtVisitor);
        String actualRes = stmtVisitor.toCode();

        String expectedRes = "r5 = new java_util_function_Predicate([&](java_lang_Object *param0) {" + NEW_LINE
                + "            java_lang_String *in0 = (java_lang_String*) param0;" + NEW_LINE
                + "            bool tmp = java_lang_Boolean::getBoolean(in0);" + NEW_LINE
                + "            return tmp;" + NEW_LINE
                + "        });" + NEW_LINE;
        Assertions.assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testInstanceMethodRef() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestDynamicInvokeHandle", "testInstanceMethodRef", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());

        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        Assertions.assertTrue(DYNAMIC_INVOKE_HANDLE.fetch(methodContext));

        DYNAMIC_INVOKE_HANDLE.optimize(methodContext);

        Stmt dynamicStmt = methodContext.getStmts().get(5);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);
        dynamicStmt.accept(stmtVisitor);
        String actualRes = stmtVisitor.toCode();

        String expectedRes = "r5 = new java_util_function_Predicate([&](java_lang_Object *param0) {" + NEW_LINE
                + "            java_lang_String *in0 = (java_lang_String*) param0;" + NEW_LINE
                + "            bool tmp = in0->isEmpty();" + NEW_LINE
                + "            return tmp;" + NEW_LINE
                + "        });" + NEW_LINE;
        Assertions.assertEquals(expectedRes, actualRes);
    }
}