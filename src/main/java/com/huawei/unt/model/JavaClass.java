package com.huawei.unt.model;

import com.huawei.unt.loader.LoaderException;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import sootup.core.model.MethodModifier;
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

public class JavaClass {
    private final String className;
    private final UDFType type;
    private final boolean isLambda;

    private final Set<JavaSootField> fields = new HashSet<>();
    private final Set<JavaSootMethod> methods = new HashSet<>();
    private final Set<ClassType> supperClasses = new HashSet<>();
    private final Set<ClassType> includes = new HashSet<>();
    private final Set<ClassType> loopIncludes = new HashSet<>();

    private boolean hasArray = false;

    private boolean hasObjectField = false;

    private boolean isJsonConstructor = false;

    // use for lambda function
    public JavaClass(String className, UDFType udfType, JavaSootMethod udfMethod) {
        this.className = className;
        this.type = udfType;
        this.methods.add(udfMethod);
        ClassType udfClassType = TranslatorUtils.getClassTypeFromClassName(udfType.getBaseClass().getName());
        this.supperClasses.add(udfClassType);
        this.isLambda = true;
        TranslatorContext.SUPERCLASS_MAP.put(className, supperClasses.stream()
                .map(ClassType::toString)
                .collect(Collectors.toSet()));
    }

    public JavaClass(JavaSootClass javaSootClass, UDFType type) {
        this.className = javaSootClass.getName();
        this.type = type;

        for (JavaSootField field : javaSootClass.getFields()) {
            if (field.getType() instanceof ClassType &&
                    TranslatorContext.IGNORED_CLASSES.contains(((ClassType) field.getType()).getFullyQualifiedName())) {
                continue;
            }
            this.fields.add(field);
        }

        for (JavaSootMethod method : javaSootClass.getMethods()) {
            if (method.isNative()) {
                throw new LoaderException("Not support native method now");
            }
            if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
                continue;
            }
            if (method.isMain(JavaIdentifierFactory.getInstance())) {
                continue;
            }
            if (TranslatorUtils.isIgnoredMethod(method)) {
                continue;
            }
            if (method.getReturnType() instanceof ClassType &&
                    TranslatorContext.IGNORED_CLASSES.contains(((ClassType) method.getReturnType()).getFullyQualifiedName())){
                continue;
            }

            boolean isIgnoreParam = false;

            for (Type parameterType : method.getParameterTypes()) {
                if (parameterType instanceof ClassType &&
                        TranslatorContext.IGNORED_CLASSES.contains(((ClassType) parameterType).getFullyQualifiedName())){
                    isIgnoreParam = true;
                    break;
                }
            }
            if (isIgnoreParam){
                continue;
            }
            this.methods.add(method);
        }

        Optional<JavaClassType> superClass = javaSootClass.getSuperclass();

        if (superClass.isPresent() &&
                !TranslatorContext.IGNORED_CLASSES.contains(superClass.get().getFullyQualifiedName())) {
            supperClasses.add(superClass.get());
        }

        supperClasses.addAll(javaSootClass.getInterfaces().stream()
                .filter(c -> !TranslatorContext.IGNORED_CLASSES.contains(c.getFullyQualifiedName()))
                .collect(Collectors.toList()));

        TranslatorContext.SUPERCLASS_MAP.put(className, supperClasses.stream()
                .map(ClassType::toString)
                .collect(Collectors.toSet()));

        this.isLambda = false;
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

    public void addLoopInclude(ClassType classType){
        this.loopIncludes.add(classType);
    }

    public void addIncludes(Set<ClassType> includes) {
        this.includes.addAll(includes);
    }

    public void addMethods(Set<JavaSootMethod> javaMethods) {
        methods.addAll(javaMethods);
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

    public void setHasObjectField(){
        hasObjectField = true;
    }

    public boolean isHasObjectField(){
        return hasObjectField;
    }

    public void setJsonConstructor(){
        isJsonConstructor = true;
    }

    public boolean isJsonConstructor(){
        return isJsonConstructor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaClass javaClass = (JavaClass) o;
        return Objects.equals(className, javaClass.className) && Objects.equals(type, javaClass.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, type);
    }
}
