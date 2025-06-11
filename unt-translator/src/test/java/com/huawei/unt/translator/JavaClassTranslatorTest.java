package com.huawei.unt.translator;

import com.huawei.unt.BaseTest;
import com.huawei.unt.dependency.DependencyAnalyzer;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.type.NoneUDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.OTFCompileAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaClassTranslatorTest extends BaseTest{
    @Test
    public void testInheritanceTranslate() throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestInheritance.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);

        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(Paths.get("src/test/resources/translator/source/TestInheritance.java"));
        JavaView view = new JavaView(inputLocation);
        JarHandler jarHandler = new JarHandler(view);
        ArrayList<JavaClass> allClasses = new ArrayList<>();
        for (JavaSootClass clz : jarHandler.getAllJavaClasses()) {
            allClasses.add(new JavaClass(clz, NoneUDF.INSTANCE));
        }

        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(jarHandler, allClasses);

        Collection<JavaClass> allDependencyClasses = dependencyAnalyzer.getAllDependencyClasses();
        TranslatorContext.updateSubclassMap();
        RefAnalyzer.analyse(allDependencyClasses);
        dependencyAnalyzer.loopIncludeAnalyzer();
        ArrayList<List<String>> translatorResults = new ArrayList<>();
        for (JavaClass javaClass : allDependencyClasses) {
            List<String> res = JavaClassTranslator.translate(javaClass);
            translatorResults.add(res);
        }
        List<String> actual = translatorResults.get(0);
        String actualString = actual.toString();
        Assertions.assertTrue(expectString.equals(actualString));
    }

    @Test
    public void testFieldTranslate() throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestFieldTranslate.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);

        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(Paths.get("src/test/resources/translator/source/TestFieldTranslate.java"));
        JavaView view = new JavaView(inputLocation);
        JarHandler jarHandler = new JarHandler(view);
        ArrayList<JavaClass> allClasses = new ArrayList<>();
        for (JavaSootClass clz : jarHandler.getAllJavaClasses()) {
            allClasses.add(new JavaClass(clz, NoneUDF.INSTANCE));
        }

        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(jarHandler, allClasses);

        Collection<JavaClass> allDependencyClasses = dependencyAnalyzer.getAllDependencyClasses();
        TranslatorContext.updateSubclassMap();
        RefAnalyzer.analyse(allDependencyClasses);
        dependencyAnalyzer.loopIncludeAnalyzer();
        ArrayList<List<String>> translatorResults = new ArrayList<>();
        for (JavaClass javaClass : allDependencyClasses) {
            List<String> res = JavaClassTranslator.translate(javaClass);
            translatorResults.add(res);
        }
        List<String> actual = translatorResults.get(0);
        String actualString = actual.toString();
//        System.out.println(actualString);
        System.out.println(actualString);
        System.out.println(expectString);
        Assertions.assertTrue(expectString.equals(actualString));
    }

    @Test
    public void testStaticFieldTranslate() throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestStaticFieldTranslator.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);

        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(Paths.get("src/test/resources/translator/source/TestStaticFieldTranslator.java"));
        JavaView view = new JavaView(inputLocation);
        JarHandler jarHandler = new JarHandler(view);
        ArrayList<JavaClass> allClasses = new ArrayList<>();
        for (JavaSootClass clz : jarHandler.getAllJavaClasses()) {
            allClasses.add(new JavaClass(clz, NoneUDF.INSTANCE));
        }

        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(jarHandler, allClasses);

        Collection<JavaClass> allDependencyClasses = dependencyAnalyzer.getAllDependencyClasses();
        TranslatorContext.updateSubclassMap();
        RefAnalyzer.analyse(allDependencyClasses);
        dependencyAnalyzer.loopIncludeAnalyzer();
        ArrayList<List<String>> translatorResults = new ArrayList<>();
        for (JavaClass javaClass : allDependencyClasses) {
            List<String> res = JavaClassTranslator.translate(javaClass);
            translatorResults.add(res);
        }
        List<String> actual = translatorResults.get(0);
        String actualString = actual.toString();
//        System.out.println(actualString);
        Assertions.assertTrue(expectString.equals(actualString));
    }

}
