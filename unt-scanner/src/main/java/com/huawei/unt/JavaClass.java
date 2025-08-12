/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JavaClass
 *
 * @since 2025-05-19
 */
public class JavaClass {
    private final Set<JavaSootField> fields = new HashSet<>();
    private final Set<JavaSootMethod> methods = new HashSet<>();
    private final Set<ClassType> supperClasses = new HashSet<>();
    private final String className;

    public JavaClass(JavaSootClass javaSootClass) {
        this.className = javaSootClass.getName();
        this.fields.addAll(javaSootClass.getFields());
        this.methods.addAll(javaSootClass.getMethods());
        Optional<JavaClassType> superClass = javaSootClass.getSuperclass();
        superClass.ifPresent(supperClasses::add);
        supperClasses.addAll(javaSootClass.getInterfaces());
    }

    public String getClassName() {
        return className;
    }

    public Set<JavaSootField> getFields() {
        return fields;
    }

    public Set<JavaSootMethod> getMethods() {
        return methods;
    }

    public Set<ClassType> getSupperClasses() {
        return supperClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JavaClass javaClass = (JavaClass) o;
        return Objects.equals(className, javaClass.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }
}
