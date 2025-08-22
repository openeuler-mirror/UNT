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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        testPojoAbstract(
                Paths.get("src/test/resources/translator/source/TestInheritance.java"),
                Paths.get("src/test/resources/translator/expect/TestInheritance.txt")
        );
    }

    @Test
    public void testFieldTranslate() throws IOException {
        testPojoAbstract(
                Paths.get("src/test/resources/translator/source/TestFieldTranslate.java"),
                Paths.get("src/test/resources/translator/expect/TestFieldTranslate.txt")
        );
    }

    @Test
    public void testStaticFieldTranslate() throws IOException {
        testPojoAbstract(
                Paths.get("src/test/resources/translator/source/TestStaticFieldTranslator.java"),
                Paths.get("src/test/resources/translator/expect/TestStaticFieldTranslator.txt")
        );
    }

    @Test
    public void testPojoTest() throws IOException {
        testPojoAbstract(
                Paths.get("src/test/resources/translator/source/TestPojo.java"),
                Paths.get("src/test/resources/translator/expect/TestPojo.txt")
        );
    }

    @Test
    public void testPojoReflectTest() throws IOException {
        testPojoAbstract(
                Paths.get("src/test/resources/translator/source/TestPojoReflect.java"),
                Paths.get("src/test/resources/translator/expect/TestPojoReflect.txt")
        );
    }

    /**
     * Tests the translation of Java classes starting from a given input file
     * (e.g., a single Java source file or compiled class file). The method analyzes
     * dependencies, translates each discovered class, and compares the generated
     * outputs against a set of expected output files.
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Wrap the single {@code inputLocation} file in an
     *       {@link OTFCompileAnalysisInputLocation} and create a {@link JavaView}.</li>
     *   <li>Use a {@link JarHandler} to enumerate classes accessible from the
     *       input file and wrap them as {@link JavaClass} objects with a default UDF.</li>
     *   <li>Initialize a {@link DependencyAnalyzer} to resolve all transitive
     *       dependencies starting from these classes.</li>
     *   <li>Update the global subclass map
     *       ({@link TranslatorContext#updateSubclassMap()}) and perform reference
     *       analysis across the dependency set.</li>
     *   <li>Run {@link DependencyAnalyzer#loopIncludeAnalyzer()} to process cyclic
     *       or looped dependencies if present.</li>
     *   <li>Translate each {@link JavaClass} into a list of strings using
     *       {@link JavaClassTranslator#translate(JavaClass)}.</li>
     *   <li>For each expected output file path, read its contents and assert that
     *       it matches the corresponding translated output.</li>
     * </ol>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>This method expects a single file input rather than a directory or
     *       archive; the analysis scope is limited to that file and its reachable
     *       dependencies.</li>
     *   <li>Updating the subclass map mutates global state and may affect other
     *       tests if not reset between runs.</li>
     *   <li>The current implementation compares outputs using
     *       {@code List.toString()}, which may not reflect exact line-based content.
     *       Consider normalizing outputs for more robust testing.</li>
     * </ul>
     *
     * @param inputLocation          the path to a single input file (Java source or class file)
     * @param expectOutputLocation   one or more expected output files, in the same order
     *                               as the generated translation results
     * @throws IOException           if reading the expected output files fails
     */
    public void testPojoAbstract(Path inputLocation, Path... expectOutputLocation) throws IOException {
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
            Assertions.assertEquals(new String(b, StandardCharsets.UTF_8), t.get(k).toString());
        }
    }

    @Test
    public void testPojoBooleanTest() throws IOException {
        testPojoAbstract(Paths.get("src/test/resources/translator/source/TestPojoBoolean.java"),
                Paths.get("src/test/resources/translator/expect/TestPojoBoolean.txt"),
                Paths.get("src/test/resources/translator/expect/TestPojoBoolean1.txt"));
    }

    @Test
    public void testPojoCharTest() throws IOException {
        testPojoAbstract(Paths.get("src/test/resources/translator/source/TestPojoChar.java"),
                Paths.get("src/test/resources/translator/expect/TestPojoChar.txt"),
                Paths.get("src/test/resources/translator/expect/TestPojoChar1.txt"),
                Paths.get("src/test/resources/translator/expect/TestPojoChar2.txt"));
    }

    @Test
    public void testPojoByteTest() throws IOException {
        testPojoAbstract(Paths.get("src/test/resources/translator/source/TestPojoByte.java"),
                Paths.get("src/test/resources/translator/expect/TestPojoByte.txt"),
                Paths.get("src/test/resources/translator/expect/TestPojoByte1.txt"));
    }
}
