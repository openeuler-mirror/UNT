/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.model;

import com.huawei.unt.loader.LoaderException;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;

import sootup.core.model.MethodModifier;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JavaClass
 *
 * @since 2025-05-19
 */
public class JavaClass {
    private final String className;
    private final UDFType type;
    private final boolean isLambda;
    private final Kind kind;

    private final Set<JavaSootField> fields = new HashSet<>();
    private final Set<JavaSootMethod> methods = new HashSet<>();
    private final Set<ClassType> supperClasses = new HashSet<>();
    private final Set<ClassType> includes = new HashSet<>();
    private final Set<ClassType> loopIncludes = new HashSet<>();
    private JavaSootClass javaSootClass = null;

    private boolean hasArray = false;
    private boolean hasObjectField = false;
    private boolean isJsonConstructor = false;
    private boolean isAbstract = false;
    private MethodSignature refMethod = null;

    // use for lambda function
    public JavaClass(String className, UDFType udfType, JavaSootMethod udfMethod) {
        this.className = className;
        this.type = udfType;
        this.methods.add(udfMethod);
        ClassType udfClassType = TranslatorUtils.getClassTypeFromClassName(udfType.getBaseClass().getName());
        this.supperClasses.add(udfClassType);
        this.isLambda = true;
        this.kind = Kind.LAMBDA_CLASS;
        TranslatorContext.getSuperclassMap().put(className, supperClasses.stream()
                .map(ClassType::toString)
                .collect(Collectors.toSet()));
    }

    public JavaClass(JavaSootClass javaSootClass, UDFType type) {
        this.className = javaSootClass.getName();
        this.type = type;
        this.javaSootClass = javaSootClass;
        this.kind = Kind.SIMPLE_CLASS;
        for (JavaSootField field : javaSootClass.getFields()) {
            if (field.getType() instanceof ClassType
                    && TranslatorContext.getIgnoredClasses().contains(
                            ((ClassType) field.getType()).getFullyQualifiedName())) {
                continue;
            }
            this.fields.add(field);
        }

        for (JavaSootMethod method : javaSootClass.getMethods()) {
            if (method.isNative()) {
                throw new LoaderException("Not support native method now");
            }
            if (method.getModifiers().contains(MethodModifier.BRIDGE)
                    || method.isMain(JavaIdentifierFactory.getInstance())
                    || TranslatorUtils.isIgnoredMethod(method)
                    || (method.getReturnType() instanceof ClassType
                        && TranslatorContext.getIgnoredClasses().contains(
                            ((ClassType) method.getReturnType()).getFullyQualifiedName()))) {
                continue;
            }

            boolean isIgnoreParam = false;
            for (Type parameterType : method.getParameterTypes()) {
                if (parameterType instanceof ClassType
                        && TranslatorContext.getIgnoredClasses().contains(
                                ((ClassType) parameterType).getFullyQualifiedName())) {
                    isIgnoreParam = true;
                    break;
                }
            }
            if (isIgnoreParam) {
                continue;
            }
            this.methods.add(method);
        }

        Optional<JavaClassType> superClass = javaSootClass.getSuperclass();

        if (superClass.isPresent()
                && !TranslatorContext.getIgnoredClasses().contains(superClass.get().getFullyQualifiedName())) {
            supperClasses.add(superClass.get());
        }

        supperClasses.addAll(javaSootClass.getInterfaces().stream()
                .filter(c -> !TranslatorContext.getIgnoredClasses().contains(c.getFullyQualifiedName()))
                .collect(Collectors.toList()));

        Set<String> superClassesSet = supperClasses.stream()
                .map(ClassType::toString)
                .collect(Collectors.toSet());
        superClassesSet.add("java.lang.Object");
        TranslatorContext.getSuperclassMap().put(className, superClassesSet);

        this.isLambda = false;
        this.isAbstract = !javaSootClass.isConcrete();
    }

    public JavaClass(MethodSignature methodSignature, UDFType udfType, Kind kind) {
        this.className = TranslatorUtils.formatMethodRefUdfClassName(methodSignature);
        this.type = udfType;
        this.kind = kind;
        this.isLambda = true;
        this.refMethod = methodSignature;
        ClassType udfClassType = TranslatorUtils.getClassTypeFromClassName(udfType.getBaseClass().getName());
        supperClasses.add(udfClassType);
        includes.addAll(udfType.getRequiredIncludes());
        includes.add(methodSignature.getDeclClassType());
    }

    public String getClassName() {
        return className;
    }

    public Set<JavaSootField> getFields() {
        return fields;
    }

    public Set<ClassType> getLoopIncludes() {
        return loopIncludes;
    }

    /**
     * add loop include
     *
     * @param classType loop include
     */
    public void addLoopInclude(ClassType classType) {
        this.loopIncludes.add(classType);
    }

    /**
     * add extra includes
     *
     * @param includes extra includes
     */
    public void addIncludes(Set<ClassType> includes) {
        this.includes.addAll(includes);
    }

    public UDFType getType() {
        return type;
    }

    public Set<JavaSootMethod> getMethods() {
        return methods;
    }

    public Set<ClassType> getSupperClasses() {
        return supperClasses;
    }

    public Set<ClassType> getIncludes() {
        return includes;
    }

    public boolean isLambda() {
        return isLambda;
    }

    public void setHasArray() {
        hasArray = true;
    }

    public boolean isHasArray() {
        return hasArray;
    }

    public void setHasObjectField() {
        hasObjectField = true;
    }

    public boolean isHasObjectField() {
        return hasObjectField;
    }

    public void setJsonConstructor() {
        isJsonConstructor = true;
    }

    public boolean isJsonConstructor() {
        return isJsonConstructor;
    }

    public void setJavaSootClass(JavaSootClass javaSootClass) {
        this.javaSootClass = javaSootClass;
    }

    public JavaSootClass getJavaSootClass() {
        return javaSootClass;
    };

    public MethodSignature getRefMethod() {
        return refMethod;
    }

    public Kind getKind() {
        return kind;
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
        return Objects.equals(className, javaClass.className) && Objects.equals(type, javaClass.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, type);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public enum Kind {
        STATIC_METHOD_REF(1, "STATIC_METHOD_REF"),
        INSTANCE_METHOD_REF(2, "INSTANCE_METHOD_REF"),
        LAMBDA_CLASS(3, "LAMBDA_CLASS"),
        SIMPLE_CLASS(2, "SIMPLE_CLASS");

        private final int val;
        private final String valStr;

        private Kind(int val, String valStr) {
            this.val = val;
            this.valStr = valStr;
        }

        public String toString() {
            return this.valStr;
        }

        public int getValue() {
            return this.val;
        }

        public String getValueName() {
            return this.valStr;
        }

        public static Kind getKind(int kind) {
            Kind[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Kind k = var1[var3];
                if (k.getValue() == kind) {
                    return k;
                }
            }

            throw new RuntimeException("Error: No javaClass kind for value '" + kind + "'.");
        }

        public static Kind getKind(String kindName) {
            Kind[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Kind k = var1[var3];
                if (k.getValueName().equals(kindName)) {
                    return k;
                }
            }

            throw new RuntimeException("Error: No method handle kind for value name '" + kindName + "'.");
        }
    }
}
