/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.loader;

import com.huawei.unt.UNTException;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.UDFType;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.transform.BodyInterceptor;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.interceptors.Aggregator;
import sootup.interceptors.CastAndReturnInliner;
import sootup.interceptors.ConstantPropagatorAndFolder;
import sootup.interceptors.CopyPropagator;
import sootup.interceptors.EmptySwitchEliminator;
import sootup.interceptors.LocalNameStandardizer;
import sootup.interceptors.LocalSplitter;
import sootup.interceptors.NopEliminator;
import sootup.interceptors.TypeAssigner;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JarHandler handle all classes in jar
 *
 * @since 2025-05-19
 */
public class JarHandler {
    private static final List<BodyInterceptor> INTERCEPTORS = ImmutableList.of(new NopEliminator(),
            new EmptySwitchEliminator(), new CastAndReturnInliner(), new LocalSplitter(), new Aggregator(),
            new CopyPropagator(), new ConstantPropagatorAndFolder(), new TypeAssigner(), new LocalNameStandardizer());

    private final JavaView javaView;
    private final TypeHierarchy typeHierarchy;
    private final Map<String, JavaSootClass> allJavaClass = new HashMap<>();

    public JarHandler(String jarPath) {
        try {
            JavaView view = getJavaView(jarPath);
            this.typeHierarchy = view.getTypeHierarchy();
            for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
                allJavaClass.put(javaSootClass.getName(), javaSootClass);
            }
            this.javaView = view;
        } catch (Exception e) {
            throw new UNTException("Can not create JarHandler, " + e.getMessage());
        }
    }

    public JarHandler(JavaView view) {
        this.typeHierarchy = view.getTypeHierarchy();
        for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
            allJavaClass.put(javaSootClass.getName(), javaSootClass);
        }
        this.javaView = view;
    }

    /**
     * check if class is other class's subclass
     *
     * @param className class need check
     * @param superClassType check super class
     * @return it's subclass or not
     */
    public boolean isSubClass(String className, ClassType superClassType) {
        if (!allJavaClass.containsKey(className)) {
            return false;
        }

        // if superClass is not in jar, check super class name is in checked superclasses or not
        if (!typeHierarchy.contains(superClassType)) {
            JavaSootClass javaClass = allJavaClass.get(className);
            if (javaClass.getSuperclass().isPresent()
                    && javaClass.getSuperclass().get().getFullyQualifiedName()
                        .equals(superClassType.getFullyQualifiedName())) {
                return true;
            }

            for (ClassType inter : javaClass.getInterfaces()) {
                if (inter.getFullyQualifiedName().equals(superClassType.getFullyQualifiedName())) {
                    return true;
                }
            }

            return false;
        }

        JavaClassType classType = JavaIdentifierFactory.getInstance().getClassType(className);
        if (typeHierarchy.contains(classType)) {
            return typeHierarchy.isSubtype(superClassType, classType);
        } else {
            return false;
        }
    }

    /**
     * Get all java classes in jar
     *
     * @return all java classes
     */
    public Collection<JavaSootClass> getAllJavaClasses() {
        return allJavaClass.values();
    }

    /**
     * GetJavaClass by ClassType
     *
     * @param classType classType
     * @param udfType udfType
     * @return JavaClass
     */
    public JavaClass getJavaClass(ClassType classType, UDFType udfType) {
        String className = classType.getFullyQualifiedName();

        if (allJavaClass.containsKey(className)) {
            JavaSootClass sootClass = allJavaClass.get(className);

            return new JavaClass(sootClass, udfType);
        } else {
            throw new LoaderException("Can not find class in jar: " + classType.getFullyQualifiedName());
        }
    }

    /**
     * try to get method from jar
     *
     * @param className className
     * @param methodName methodName
     * @param retType return type
     * @param params param types
     * @return optional JavaSootMethod
     */
    public Optional<JavaSootMethod> tryGetMethod(String className, String methodName,
            String retType, List<String> params) {
        ClassType classType = javaView.getIdentifierFactory().getClassType(className);

        MethodSignature signature = javaView.getIdentifierFactory()
                .getMethodSignature(classType, methodName, retType, params);

        return javaView.getMethod(signature);
    }

    private static JavaView getJavaView(String jarPath) {
        if (jarPath.endsWith(".jar")) {
            File file = new File(jarPath);
            if (file.exists()) {
                AnalysisInputLocation location =
                        new JavaClassPathAnalysisInputLocation(jarPath, SourceType.Library, INTERCEPTORS);
                return new JavaView(location);
            } else {
                throw new TranslatorException("Jar file is not exists");
            }
        } else {
            throw new TranslatorException("Only support translate jar file now");
        }
    }
}
