package com.huawei.unt.type.flink;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapFunctionTest {
    private static JarHandler jarHandler;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/function/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, BaseTest.INTERCEPTORS);

        JavaView view = new JavaView(inputLocation);

        jarHandler = new JarHandler(view);
    }

    @Test
    public void testBaseClass() {
        assertEquals(FlinkMapFunction.INSTANCE.getBaseClass().getName(), "org.apache.flink.api.common.functions.MapFunction");
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
