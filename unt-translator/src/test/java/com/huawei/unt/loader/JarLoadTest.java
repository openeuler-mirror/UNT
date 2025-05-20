package com.huawei.unt.loader;

import com.huawei.unt.BaseTest;
import com.huawei.unt.type.EngineType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JarLoadTest extends BaseTest {
    private static JarHandler JAR_HANDLER;

    @BeforeAll
    public static void initJarHandler() {
        Path jarPath = Paths.get("src/test/resources/loader/binary/loaderTestcase.jar");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(jarPath, SourceType.Library, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        JAR_HANDLER = new JarHandler(javaView);
    }

    @Test
    public void testLoad() {
        assertEquals(3, JAR_HANDLER.getAllJavaClasses().size());
    }

    @Test
    public void testLoadUDF() {
        JarUdfLoader jarUdfLoader = new JarUdfLoader(JAR_HANDLER, EngineType.FLINK);

        jarUdfLoader.loadUdfClasses();

        assertEquals(2, jarUdfLoader.getClassUdfMap().size());
    }


}
