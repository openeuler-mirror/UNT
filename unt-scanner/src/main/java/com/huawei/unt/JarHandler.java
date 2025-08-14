/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handle all java classes in jar
 *
 * @since 2025-05-19
 */
public class JarHandler {
    private static final String FLINK_PREFIX = "org.apache.flink";

    private final Map<String, JavaSootClass> allJavaClass = new HashMap<>();

    public JarHandler(String jarPath) {
        JavaView view = getJavaView(jarPath);
        for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
            allJavaClass.put(javaSootClass.getName(), javaSootClass);
        }
    }

    /**
     * check class is in jar or not
     *
     * @param className className
     * @return class in jar or not
     */
    public boolean containsClass(String className) {
        return allJavaClass.containsKey(className);
    }

    /**
     * Get JavaClass from jar by className
     *
     * @param className className
     * @return JavaClass
     */
    public JavaClass getJavaClassByName(String className) {
        return new JavaClass(allJavaClass.get(className));
    }

    private static JavaView getJavaView(String jarPath) {
        if (jarPath.endsWith(".jar")) {
            File file = new File(jarPath);
            if (file.exists()) {
                AnalysisInputLocation location =
                        new JavaClassPathAnalysisInputLocation(jarPath, SourceType.Library);
                return new JavaView(location);
            } else {
                throw new UNTException("Jar file is not exists");
            }
        } else {
            throw new UNTException("Only support translate jar file now");
        }
    }

    /**
     * load udf classes form jar
     *
     * @return udf classes in jar
     */
    public Set<JavaClass> loadUdfClasses() {
        Set<JavaClass> udfClasses = new HashSet<>();

        for (JavaSootClass javaClass : allJavaClass.values()) {
            if (javaClass.getName().startsWith(FLINK_PREFIX)) {
                continue;
            }

            if (isUdfType(javaClass)) {
                udfClasses.add(new JavaClass(javaClass));
            }
        }

        return udfClasses;
    }

    private boolean isUdfType(JavaSootClass clz) {
        if (clz.getSuperclass().isPresent()
                && UNTConstant.FLINK_UDF_CLASSES.contains(clz.getSuperclass().get().getFullyQualifiedName())) {
            return true;
        }

        for (ClassType in : clz.getInterfaces()) {
            if (UNTConstant.FLINK_UDF_CLASSES.contains(in.getFullyQualifiedName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * check if class is udf class or not
     *
     * @param clz class name
     * @return class is udf class or not
     */
    public boolean isUdfType(String clz) {
        return UNTConstant.FLINK_UDF_CLASSES.contains(clz);
    }
}
