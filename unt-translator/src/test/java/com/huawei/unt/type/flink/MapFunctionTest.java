/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type.flink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.huawei.unt.BaseTest;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Test MapFunction
 *
 * @since 2025-05-19
 */
public class MapFunctionTest {
    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/function/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, BaseTest.INTERCEPTORS);

        JavaView view = new JavaView(inputLocation);
    }

    @Test
    public void testBaseClass() {
        assertEquals(FlinkMapFunction.INSTANCE.getBaseClass().getName(),
                "org.apache.flink.api.common.functions.MapFunction");
    }

    @Test
    public void testGetSoPrefix() {
        assertEquals(FlinkMapFunction.INSTANCE.getSoPrefix(), "libmap");
    }

    @Test
    public void testGetRequiredIncludes() {
        Set<ClassType> required = FlinkMapFunction.INSTANCE.getRequiredIncludes();
        assertEquals(required.size(), 1);
    }
}
