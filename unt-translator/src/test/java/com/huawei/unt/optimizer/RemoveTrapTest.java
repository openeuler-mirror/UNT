/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Test RemoveTrap optimizer
 *
 * @since 2025-05-19
 */
public class RemoveTrapTest extends BaseTest {
    private static MethodContext methodContext;

    private final RemoveTrap opt = new RemoveTrap();

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        JarHandler jarHandler = new JarHandler(javaView);

        Optional<JavaSootMethod> method =
                jarHandler.tryGetMethod("TestTrap", "checkException", "void", ImmutableList.of());

        methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
    }

    @Test
    public void testFetch() {
        assertTrue(opt.fetch(methodContext));
    }

    @Test
    public void testOptimize() {
        Optimizer optimizer = new RemoveTrap();
        optimizer.optimize(methodContext);

        List<Integer> removedStmt = ImmutableList.of(4, 5, 6, 7, 9, 10);

        for (int index : removedStmt) {
            assertTrue(methodContext.isRemoved(index));
        }
    }
}
