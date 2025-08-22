/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import com.huawei.unt.BaseTest;
import com.huawei.unt.dependency.DependencyAnalyzer;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.type.NoneUDF;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.OTFCompileAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test JavaClassTranslatorTest
 *
 * @since 2025-06-11
 */
public class JavaClassTranslatorTest extends BaseTest {
    @Test
    public void testInheritanceTranslate() throws IOException {
        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(
                Paths.get("src/test/resources/translator/source/TestInheritance.java"));
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
        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestInheritance.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertEquals (expectString, actualString);
    }

    @Test
    public void testFieldTranslate() throws IOException {
        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(
                Paths.get("src/test/resources/translator/source/TestFieldTranslate.java"));
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
        byte[] bytes = Files.readAllBytes(
                Paths.get("src/test/resources/translator/expect/TestFieldTranslate.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertEquals (expectString, actualString);
    }

    @Test
    public void testStaticFieldTranslate() throws IOException {
        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(
                Paths.get("src/test/resources/translator/source/TestStaticFieldTranslator.java"));
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
        byte[] bytes = Files.readAllBytes(
                Paths.get("src/test/resources/translator/expect/TestStaticFieldTranslator.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertEquals (expectString, actualString);
    }

    @Test
    public void testPojoTest() throws IOException {
        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(
                Paths.get("src/test/resources/translator/source/TestPojo.java"));
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
        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestPojo.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertEquals (expectString, actualString);

        System.err.println (translatorResults.size ());
        System.err.println (translatorResults.get (1));
    }

    @Test
    public void testPojoReflectTest() throws IOException {
        AnalysisInputLocation inputLocation = new OTFCompileAnalysisInputLocation(
                Paths.get("src/test/resources/translator/source/TestPojoReflect.java"));
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
        byte[] bytes = Files.readAllBytes(Paths.get("src/test/resources/translator/expect/TestPojoReflect.txt"));
        String expectString = new String(bytes, StandardCharsets.UTF_8);

        Assertions.assertEquals (expectString, actualString);
    }

    public void testPojoAbstract (Path inputLocation, Path ... expectOutputLocation) throws IOException {
        AnalysisInputLocation i = new OTFCompileAnalysisInputLocation(inputLocation);
        JavaView v = new JavaView(i);
        JarHandler r = new JarHandler(v);
        ArrayList<JavaClass> a = new ArrayList<>();
        for (JavaSootClass s : r.getAllJavaClasses()) {
            a.add(new JavaClass(s, NoneUDF.INSTANCE));
        }
        DependencyAnalyzer d = new DependencyAnalyzer(r, a);
        Collection<JavaClass> c = d.getAllDependencyClasses();
        TranslatorContext.updateSubclassMap();
        RefAnalyzer.analyse(c);
        d.loopIncludeAnalyzer();
        ArrayList<List<String>> t = new ArrayList<>();
        for (JavaClass j : c) {
            List<String> u = JavaClassTranslator.translate(j);
            t.add(u);
        }
        for (int k = 0; k < expectOutputLocation.length; k++) {
            Path e = expectOutputLocation[k];
            byte[] b = Files.readAllBytes(e);
            Assertions.assertEquals (new String (b, StandardCharsets.UTF_8), t.get (k).toString ());
        }
    }

    @Test
    public void testPojoBooleanTest () throws IOException {
        testPojoAbstract (Paths.get ("src/test/resources/translator/source/TestPojoBoolean.java"),
                Paths.get ("src/test/resources/translator/expect/TestPojoBoolean.txt"),
                Paths.get ("src/test/resources/translator/expect/TestPojoBoolean1.txt"));
    }

    @Test
    public void testPojoCharTest () throws IOException {
        testPojoAbstract (Paths.get ("src/test/resources/translator/source/TestPojoChar.java"),
                Paths.get ("src/test/resources/translator/expect/TestPojoChar.txt"),
                Paths.get ("src/test/resources/translator/expect/TestPojoChar1.txt"),
                Paths.get ("src/test/resources/translator/expect/TestPojoChar2.txt"));
    }

    @Test
    public void testPojoByteTest () throws IOException {
        testPojoAbstract (Paths.get ("src/test/resources/translator/source/TestPojoByte.java"),
                Paths.get ("src/test/resources/translator/expect/TestPojoByte.txt"),
                Paths.get ("src/test/resources/translator/expect/TestPojoByte1.txt"));
    }
}
