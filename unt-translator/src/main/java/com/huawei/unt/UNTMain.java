/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import com.huawei.unt.dependency.DependencyAnalyzer;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.loader.JarUdfLoader;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.JavaClassTranslator;
import com.huawei.unt.translator.RefAnalyzer;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.EngineType;
import com.huawei.unt.type.UDFType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Class for unt translator
 *
 * @since 2025-05-19
 */
public class UNTMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(UNTMain.class);

    private static final String SHA256 = "SHA256";

    public static void main(String[] args) {
        // arg0: jarPath
        // arg1: engineType
        // arg2: outputBasePath
        String jarPath = args[0];
        String engineType = args[1];
        String basePath = args[2];
        String jarPathHash = TranslatorUtils.getJarHashPath(jarPath);

        LOGGER.info(String.format("translate logs for jar %s start", jarPathHash));

        EngineType engine = TranslatorUtils.getEngineType(engineType);

        String confDir = basePath + File.separator + "conf";
        TranslatorContext.init(confDir);

        JarHandler jarHandler = new JarHandler(jarPath);

        JarUdfLoader jarUdfLoader = new JarUdfLoader(jarHandler, engine);

        jarUdfLoader.loadUdfClasses();

        Map<UDFType, List<JavaClass>> classesUdfMap = jarUdfLoader.getClassUdfMap();

        List<JavaClass> allClassUDFs = new ArrayList<>();

        for (List<JavaClass> classes : classesUdfMap.values()) {
            allClassUDFs.addAll(classes);
        }

        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(jarHandler, allClassUDFs);

        HashMap<JavaClass, List<String>> codeMap = new HashMap<>();

        Collection<JavaClass> allDependencyClasses = dependencyAnalyzer.getAllDependencyClasses();
        TranslatorContext.updateSubclassMap();
        RefAnalyzer.analyse(allDependencyClasses);
        dependencyAnalyzer.loopIncludeAnalyzer();
        for (JavaClass javaClass : allDependencyClasses) {
            try {
                codeMap.put(javaClass, JavaClassTranslator.translate(javaClass));
            } catch (TranslatorException e) {
                LOGGER.error("Translate {} failed, {}", javaClass.getClassName(), e.getMessage());
                throw e;
            }
        }

        File output = new File(basePath);

        if (!output.exists() || !output.isDirectory()) {
            throw new UNTException("output dictionary need an exists dictionary");
        }

        String cppDir = basePath + File.separator + "cpp" + File.separator + jarPathHash;

        File cppDirFile = new File(cppDir);
        if (cppDirFile.exists() && !cppDirFile.isDirectory()) {
            throw new UNTException("cpp dictionary need an dictionary");
        }
        if (!cppDirFile.exists()) {
            boolean mkdir = cppDirFile.mkdirs();
            if (!mkdir) {
                throw new UNTException("Create cpp dictionary failed");
            }
        }
        List<String> allCppFiles = new ArrayList<>();
        // output head files
        for (Map.Entry<JavaClass, List<String>> codeEntry : codeMap.entrySet()) {
            JavaClass javaClass = codeEntry.getKey();

            String formatClassName = TranslatorUtils.formatClassName(javaClass.getClassName());
            String udfHeadFile = cppDir + File.separator + formatClassName + ".h";
            String udfCppFile = cppDir + File.separator + formatClassName + ".cpp";

            allCppFiles.add(udfCppFile);

            try (FileWriter headFileWriter = new FileWriter(udfHeadFile, false)) {
                headFileWriter.write(codeMap.get(javaClass).get(0));
            } catch (IOException e) {
                throw new TranslatorException("Can not create file, " + e.getMessage());
            }
            try (FileWriter cppFileWriter = new FileWriter(udfCppFile, false)) {
                cppFileWriter.write(codeMap.get(javaClass).get(1));
            } catch (IOException e) {
                throw new TranslatorException("Can not create file, " + e.getMessage());
            }
        }

        String shaFile = basePath + File.separator + SHA256;

        try (FileWriter fileWriter = new FileWriter(shaFile, false)) {
            fileWriter.write(jarPathHash);
        } catch (IOException e) {
            throw new UNTException("Can't create or write file, " + e);
        }

        if (!TranslatorContext.getMissingInterfaces().isEmpty()) {
            StringBuilder errMessage = new StringBuilder();
            errMessage.append("the following methods is missing, auto compilation is terminated!\n");
            for (String className : TranslatorContext.getMissingInterfaces().keySet()) {
                errMessage.append(className).append(": \n");
                for (String method : TranslatorContext.getMissingInterfaces().get(className)) {
                    errMessage.append(TAB).append(method).append(NEW_LINE);
                }
            }
            LOGGER.error(errMessage.toString());
            throw new TranslatorException(errMessage.toString());
        }

        String outputDir = basePath + File.separator + "output";
        String compileShell = cppDir + File.separator + "compile_translated_udf.sh";
        String udfProperties = outputDir + File.separator + jarPathHash + File.separator + "udf.properties";
        String outPath = outputDir + File.separator + jarPathHash + File.separator;

        try (FileWriter fileWriter = new FileWriter(shaFile, false)) {
            fileWriter.write(jarPathHash);
        } catch (IOException e) {
            throw new UNTException("Can't create or write file, " + e);
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("mkdir", "-p", outPath);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOGGER.info("content create success");
            } else {
                LOGGER.error("content create failed");
            }
        } catch (IOException | InterruptedException e) {
            throw new TranslatorException("can not create outPath");
        }

        String baseDir = TranslatorContext.getUdfMap().get("basic_lib_path");
        baseDir = baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator;
        String templates = basePath + File.separator + "templates";

        String mainMakefileTemplate;
        String subMakefileTemplate;
        try {
            mainMakefileTemplate = new String(Files.readAllBytes(Paths.get(templates, "main-Makefile")));
            subMakefileTemplate = new String(Files.readAllBytes(Paths.get(templates, "sub-Makefile")));
            // makefile
            if (TranslatorContext.isIsRegexAcc()) {
                if (!TranslatorContext.getUdfMap().containsKey("regex_lib_path")) {
                    LOGGER.warn("regex lib path not exist, use default path: /usr/local/ksl/lib/libKHSEL_ops.a");
                } else {
                    String regexLibPath = TranslatorContext.getUdfMap().get("regex_lib_path");
                    File file = new File(regexLibPath);
                    if (file.exists() && !file.isDirectory()) {
                        mainMakefileTemplate = mainMakefileTemplate.replace(
                                "regex_lib := /usr/local/ksl/lib/libKHSEL_ops.a",
                                "regex_lib := " + regexLibPath);
                    } else {
                        LOGGER.warn("regex lib path not exist, use default path: /usr/local/ksl/lib/libKHSEL_ops.a");
                    }
                }
            } else {
                mainMakefileTemplate = mainMakefileTemplate
                        .replace("regex_lib := /usr/local/ksl/lib/libKHSEL_ops.a", "");
                mainMakefileTemplate = mainMakefileTemplate.replace("regex_lib", "");
                subMakefileTemplate = subMakefileTemplate.replace("$(regex_lib)", "");
            }
            // MakeFile
            mainMakefileTemplate = mainMakefileTemplate.replace("CXXFLAGS := -O3 -std=c++17 -fPIC",
                    "CXXFLAGS := " + TranslatorContext.getCompileOption());
        } catch (IOException e) {
            throw new TranslatorException("load Makefile templates failed: " + e.getMessage());
        }

        String mainMakefile = cppDir + File.separator + "Makefile";
        try (FileWriter mainMakefileWriter = new FileWriter(mainMakefile, false)) {
            Map<String, String> mainProperties = new HashMap<>();
            mainProperties.put("basicPath", baseDir);
            mainProperties.put("sha256", jarPathHash);
            String mainMakefileContent = mainMakefileTemplate;
            for (Map.Entry<String, String> property : mainProperties.entrySet()) {
                mainMakefileContent = mainMakefileContent.replace("${" + property.getKey() + "}", property.getValue());
            }
            mainMakefileWriter.write(mainMakefileContent);
        } catch (IOException e) {
            throw new TranslatorException("generate main makefile failed: " + e.getMessage());
        }

        try (FileWriter compileShellWriter = new FileWriter(compileShell, false);
             FileWriter udfPropertiesWriter = new FileWriter(udfProperties, false)) {
            for (UDFType udfType : classesUdfMap.keySet()) {
                List<JavaClass> classes = classesUdfMap.get(udfType);
                for (int i = 0; i < classes.size(); i++) {
                    if (classes.get(i).isAbstract()) {
                        continue;
                    }
                    String formatClassName = TranslatorUtils.formatClassName(classes.get(i).getClassName());
                    String nativeDir = cppDir + File.separator + formatClassName;
                    String udfCppFile = nativeDir + File.separator + formatClassName + "_native.cpp";

                    File nativeDirFile = new File(nativeDir);
                    if (nativeDirFile.exists() && !nativeDirFile.isDirectory()) {
                        throw new UNTException("native dictionary need an dictionary");
                    }
                    if (!nativeDirFile.exists()) {
                        boolean isMkdir = nativeDirFile.mkdirs();
                        if (!isMkdir) {
                            throw new UNTException("Create native dictionary failed");
                        }
                    }

                    try (FileWriter cppFileWriter = new FileWriter(udfCppFile, false)) {
                        cppFileWriter.write(udfType.getCppFileString(formatClassName));
                    } catch (IOException e) {
                        throw new TranslatorException("Can not create file, " + e.getMessage());
                    }

                    String subMakefile = nativeDir + File.separator + "Makefile";
                    String soFileName = udfType.getSoPrefix() + "_" + formatClassName;
                    String subMakefileContent = subMakefileTemplate.replace("${soFileName}", soFileName);
                    try (FileWriter subMakefileWriter = new FileWriter(subMakefile, false)) {
                        subMakefileWriter.write(subMakefileContent);
                    } catch (IOException e) {
                        throw new TranslatorException("generate makefile for " + formatClassName + " failed: "
                                + e.getMessage());
                    }

                    String includePathBasic = (baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator)
                            + "include";

                    String basicLib = (baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator)
                            + "lib" + File.separator + "libbasictypes.a";

                    String regexLib = (baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator)
                            + "lib" + File.separator + "libKHSEL_ops.a";

                    String compileCommand = "g++ -O3 -std=c++17 " + "-I" + includePathBasic
                            + " -shared -fPIC " + udfCppFile;

                    for (String allCppFile : allCppFiles) {
                        compileCommand = compileCommand + " " + allCppFile;
                    }
                    compileCommand += " " + basicLib + " " + regexLib + " -o "
                            + outputDir + File.separator + jarPathHash + File.separator + soFileName + ".so"
                            + NEW_LINE;

                    String propertyLine = classes.get(i).getClassName() + "=" + soFileName + ".so"
                            + NEW_LINE;
                    compileShellWriter.write(compileCommand);
                    udfPropertiesWriter.write(propertyLine);
                }
            }
        } catch (IOException e) {
            throw new TranslatorException("Can not create sub makefile, " + e.getMessage());
        }
    }
}
