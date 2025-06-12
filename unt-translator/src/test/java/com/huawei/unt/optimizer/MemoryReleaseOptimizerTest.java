/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Test MemoryReleaseOptimizer optimizer
 *
 * @since 2025-06-10
 */
class MemoryReleaseOptimizerTest extends BaseTest {
    private static final MemoryReleaseOptimizer MEMORY_RELEASE_OPTIMIZER = new MemoryReleaseOptimizer();
    private static JarHandler jarHandler;
    private static Map<String, Integer> refMap;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        jarHandler = new JarHandler(javaView);

        refMap = TranslatorContext.getLibInterfaceRef();
    }

    @Test
    public void testSetRet() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestMemory", "testNeedRet", "int", ImmutableList.of("int"));
        Assertions.assertTrue(method.isPresent());
        refMap.put(method.get().getSignature().toString(), 0);
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(MEMORY_RELEASE_OPTIMIZER.fetch(methodContext));

        MEMORY_RELEASE_OPTIMIZER.optimize(methodContext);

        assertTrue(methodContext.needRet());

        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            if (methodContext.hasRet(i)) {
                assertTrue("7".equals(methodContext.getRetValue(i).toString())
                        || "9".equals(methodContext.getRetValue(i).toString()));
            }
        }
    }

    @Test
    public void testGotoFree() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestMemory", "testGotoFree", "java.lang.String", ImmutableList.of("int"));
        Assertions.assertTrue(method.isPresent());
        refMap.put(method.get().getSignature().toString(), 1);
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(MEMORY_RELEASE_OPTIMIZER.fetch(methodContext));

        MEMORY_RELEASE_OPTIMIZER.optimize(methodContext);

        int gotoCount = 0;
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            if (methodContext.hasGoto(i)) {
                gotoCount++;
            }
        }
        assertEquals(1, gotoCount);
    }

    @Test
    public void testUnknownFree() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestMemory", "testUnknownFree", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());
        refMap.put(method.get().getSignature().toString(), 0);
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(MEMORY_RELEASE_OPTIMIZER.fetch(methodContext));

        MEMORY_RELEASE_OPTIMIZER.optimize(methodContext);

        int unknownFreePos = 0;
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            if (!methodContext.getUnknownFreeVars(i).isEmpty()) {
                int count = methodContext.getUnknownFreeVars(i).size();
                assertTrue(count == 1 || count == 3);
                unknownFreePos++;
            }
        }
        assertEquals(2, unknownFreePos);
    }

    @Test
    public void testLoopFree() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestMemory", "testLoopFree", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());
        refMap.put(method.get().getSignature().toString(), 0);
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(MEMORY_RELEASE_OPTIMIZER.fetch(methodContext));

        BranchStmtLabeler branchStmtLabeler = new BranchStmtLabeler();
        branchStmtLabeler.optimize(methodContext);
        MEMORY_RELEASE_OPTIMIZER.optimize(methodContext);

        int circleFreePos = 0;
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            if (!methodContext.getCircleFreeVars(i).isEmpty()) {
                int count = methodContext.getCircleFreeVars(i).size();
                assertTrue(count == 1 || count == 2);
                circleFreePos++;
            }
        }
        assertEquals(4, circleFreePos);
    }
}