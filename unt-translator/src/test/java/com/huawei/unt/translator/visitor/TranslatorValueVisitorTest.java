package com.huawei.unt.translator.visitor;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.type.NoneUDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.BooleanConstant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.model.SourceType;
import sootup.core.types.PrimitiveType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TranslatorValueVisitorTest extends BaseTest {
    private static MethodContext methodContext;
//    private static TranslatorValueVisitor valueVisitor;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        JarHandler jarHandler = new JarHandler(javaView);
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod("TestTrap", "checkException", "void", ImmutableList.of());
        methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
//        valueVisitor = new TranslatorValueVisitor(methodContext);
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


}
