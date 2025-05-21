package com.huawei.unt.optimizer;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.type.NoneUDF;
import org.junit.jupiter.api.Assertions;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BranchStmtLabelerTest extends BaseTest {
    private static final BranchStmtLabeler BRANCH_LABELER = new BranchStmtLabeler();
    private static JarHandler JAR_HANDLER;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        JAR_HANDLER = new JarHandler(javaView);
    }

    @Test
    public void testIf() {
        Optional<JavaSootMethod> method = JAR_HANDLER.tryGetMethod("TestBranch", "testIf", "void", ImmutableList.of("int"));
        Assertions.assertTrue(method.isPresent());
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(BRANCH_LABELER.fetch(methodContext));

        BRANCH_LABELER.optimize(methodContext);

        List<Integer> labels = ImmutableList.of(6, 8);

        for (int i = 0; i < labels.size(); i++) {
            assertEquals("label" + i, methodContext.getLabelString(labels.get(i)));
        }
    }

    @Test
    public void testSwitch() {
        Optional<JavaSootMethod> method = JAR_HANDLER.tryGetMethod("TestBranch", "testSwitch", "void", ImmutableList.of("int"));
        Assertions.assertTrue(method.isPresent());
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(BRANCH_LABELER.fetch(methodContext));

        BRANCH_LABELER.optimize(methodContext);

        List<Integer> labels = ImmutableList.of(3, 6, 9, 12, 15, 17);

        for (int i = 0; i < labels.size(); i++) {
            assertEquals("label" + i, methodContext.getLabelString(labels.get(i)));
        }
    }

    @Test
    public void testLoop() {
        Optional<JavaSootMethod> method = JAR_HANDLER.tryGetMethod("TestBranch", "testLoop", "void", ImmutableList.of());
        Assertions.assertTrue(method.isPresent());
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        Assertions.assertTrue(BRANCH_LABELER.fetch(methodContext));

        BRANCH_LABELER.optimize(methodContext);

        List<Integer> labels = ImmutableList.of(2, 7);

        for (int i = 0; i < labels.size(); i++) {
            assertEquals("label" + i, methodContext.getLabelString(labels.get(i)));
        }
    }
}
