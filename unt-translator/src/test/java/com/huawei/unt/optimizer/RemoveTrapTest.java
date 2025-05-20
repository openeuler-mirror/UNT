package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.type.NoneUDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveTrapTest extends BaseTest {
    private final RemoveTrap opt = new RemoveTrap();
    private static MethodContext METHOD_CONTEXT;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        JarHandler jarHandler = new JarHandler(javaView);

        Optional<JavaSootMethod> method =
                jarHandler.tryGetMethod("TestTrap", "checkException", "void", ImmutableList.of());

        METHOD_CONTEXT = new MethodContext(method.get(), NoneUDF.INSTANCE);
    }

    @Test
    public void testFetch() {
        assertTrue(opt.fetch(METHOD_CONTEXT));

    }

    @Test
    public void testOptimize() {
        Optimizer optimizer = new RemoveTrap();
        optimizer.optimize(METHOD_CONTEXT);

        List<Integer> removedStmt = ImmutableList.of(4, 5, 6, 7, 9, 10);

        for (int index : removedStmt) {
            assertTrue(METHOD_CONTEXT.isRemoved(index));
        }
    }
}
