/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.huawei.unt.BaseTest;
import com.huawei.unt.type.EngineType;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JarLoad test
 *
 * @since 2025-05-19
 */
public class JarLoadTest extends BaseTest {
    private static JarHandler jarHandler;

    @BeforeAll
    public static void initJarHandler() {
        Path jarPath = Paths.get("src/test/resources/loader/binary/loaderTestcase.jar");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(jarPath, SourceType.Library, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        jarHandler = new JarHandler(javaView);
    }

    @Test
    public void testLoad() {
        assertEquals(3, jarHandler.getAllJavaClasses().size());
    }

    @Test
    public void testLoadUDF() {
        JarUdfLoader jarUdfLoader = new JarUdfLoader(jarHandler, EngineType.FLINK);

        jarUdfLoader.loadUdfClasses();

        assertEquals(2, jarUdfLoader.getClassUdfMap().size());
    }
}
