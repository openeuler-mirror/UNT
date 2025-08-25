/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import static com.huawei.unt.model.JavaClass.Kind.INSTANCE_METHOD_REF;
import static com.huawei.unt.model.JavaClass.Kind.STATIC_METHOD_REF;
import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.NEW_OBJ;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import com.huawei.unt.model.JavaClass;
import com.huawei.unt.type.NoneUDF;
import com.huawei.unt.type.UDFType;

import com.google.common.collect.ImmutableList;

import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.model.FieldModifier;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.VoidType;
import sootup.java.core.*;
import sootup.java.core.types.JavaClassType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * JavaClassTranslator use for translate java class to cpp file
 *
 * @since 2025-05-19
 */
public class JavaClassTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaClassTranslator.class);
    private static final String ACCESS_PREFIX = "access$";
    private static final String STATIC_INIT_FIELD = "initStaticField";
    private static final String STATIC_INIT_METHOD = "initStatic";
    private static final String STATIC_METHOD_INVOKE = "%s::%s(%s);" + NEW_LINE;
    private static final String INSTANCE_METHOD_INVOKE = "%s->%s();" + NEW_LINE;

    private JavaClassTranslator() {
    }

    /**
     * translate javaClass to cpp code string
     *
     * @param javaClass javaClass
     * @return cpp code strings
     */
    public static List<String> translate(JavaClass javaClass) {
        List<String> translateRes = new ArrayList<>();
        StringBuilder headBuilder = new StringBuilder();
        StringBuilder cppBuilder = new StringBuilder();

        // print micro
        String microString = TranslatorUtils.formatMicroName(javaClass.getClassName());
        headBuilder.append("#ifndef ").append(microString).append(NEW_LINE)
                .append("#define ").append(microString).append(NEW_LINE)
                .append(NEW_LINE);

        Set<JavaSootField> fields = new HashSet<>(javaClass.getFields());
        Set<JavaSootMethod> methods = new HashSet<>(javaClass.getMethods());

        fields = fields.stream().filter(f -> !(f.getType() instanceof ClassType
                && TranslatorContext.getIgnoredClasses().contains(((ClassType) f.getType()).getFullyQualifiedName())
        )).collect(Collectors.toSet());

        Optional<JavaSootMethod> staticInitMethod = Optional.empty();
        Set<JavaSootMethod> initMethods = new HashSet<>();

        for (JavaSootMethod method : methods) {
            if (method.getName().equals(TranslatorContext.STATIC_INIT_FUNCTION_NAME)) {
                staticInitMethod = Optional.of(method);
            }
            if (method.getName().equals(TranslatorContext.INIT_FUNCTION_NAME)) {
                initMethods.add(method);
            }
        }

        staticInitMethod.ifPresent(methods::remove);
        initMethods.forEach(methods::remove);

        // dealWith includes
        headBuilder.append(TranslatorUtils.printIncludes(javaClass)).append(NEW_LINE);

        // dealWith loopIncludes
        Set<ClassType> loopIncludes = javaClass.getLoopIncludes();
        for (ClassType loopInclude : loopIncludes) {
            headBuilder.append("class ")
                    .append(TranslatorUtils.formatClassName(loopInclude.getFullyQualifiedName()))
                    .append(";")
                    .append(NEW_LINE);
        }

        // print class head
        headBuilder.append("class ").append(TranslatorUtils.formatClassName(javaClass.getClassName()));

        // deal with extends
        Set<ClassType> superClasses = javaClass.getSupperClasses().stream()
                .filter(c -> !TranslatorContext.getIgnoredClasses().contains(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (!superClasses.isEmpty()) {
            headBuilder.append(" : ");
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (ClassType superClassType : superClasses) {
            if (!TranslatorContext.getIgnoredClasses().contains(superClassType.getFullyQualifiedName())) {
                String formatType = TranslatorUtils.formatType(superClassType);
                String templates = "";
                if (TranslatorContext.getGenericFunction().containsKey(formatType)) {
                    templates = TranslatorContext.getGenericFunction().get(formatType);
                }
                joiner.add("public " + formatType + templates);
            }
        }

        headBuilder.append(joiner).append(" {").append(NEW_LINE);
        Set<JavaSootMethod> allNeedTranslateMethod = new HashSet<>(javaClass.getMethods());

        if (javaClass.isLambda()) {
            headBuilder.append("public:").append(NEW_LINE);
            headBuilder.append(TAB)
                    .append(TranslatorUtils.formatClassName(javaClass.getClassName()))
                    .append("() = default;")
                    .append(NEW_LINE);
            headBuilder.append(javaClass.getType().printLambdaDeclare());
            if (javaClass.isJsonConstructor()) {
                printConstructorFromJsonDeclare(headBuilder, javaClass);
            }

            headBuilder.append("};").append(NEW_LINE);
        } else {
            // all public/protected elements into public temp, if function start with access$ change it to public
            List<JavaSootMethod> publicMethods = methods.stream()
                    .filter(m -> m.isPublic() || m.isProtected() || m.getName().startsWith(ACCESS_PREFIX))
                    .collect(Collectors.toList());

            publicMethods.forEach(methods::remove);

            headBuilder.append("public:").append(NEW_LINE);

            //json

            Set<ClassType> superClassesJson = superClasses.stream()
                    .filter(m -> !TranslatorContext.JSON_SERIALIZE_SET.contains(m.getFullyQualifiedName()))
                    .collect(Collectors.toSet());

            //
            if (superClassesJson.size() > 0) {
                for (ClassType classType : superClassesJson) {
                    headBuilder.append(TAB).append("KACC_DEFINE_ZHUJIE_DERIVED_TYPE_INTRUSIVE_BASE_CLASS(")
                            .append(TranslatorUtils.formatClassName(javaClass.getClassName())).append(",")
                            .append(TranslatorUtils.formatType(classType))
                            .append(")").append(NEW_LINE);
                }

                headBuilder.append(TAB).append("KACC_DEFINE_ZHUJIE_DERIVED_TYPE_INTRUSIVE_MEMBER(")
                        .append(TranslatorUtils.formatClassName(javaClass.getClassName()));
                for (JavaSootField field : fields) {
                    boolean isIgnore = false;
                    String serializerName = "Not_Exist_Serializer";
                    for (AnnotationUsage annotation : field.getAnnotations()) {
                        if (annotation.getAnnotation().getClassName().equals("JsonIgnore")){
                            isIgnore = true;
                            break;
                        }else if (annotation.getAnnotation().getClassName().equals("JsonSerialize")){
                            Map<String, Object> values = annotation.getValues();
                            for (Map.Entry<String, Object> stringObjectEntry : values.entrySet()) {
                                String key = stringObjectEntry.getKey();
                                if ("using".equals(key)){
                                    Object value = stringObjectEntry.getValue();
                                    String className = ((ClassConstant) value).getValue();
                                    serializerName = TranslatorUtils.formatClassName(TranslatorUtils.parseSignature(className));
                                }
                            }
                        }
                    }
                    if (!isIgnore && !field.isStatic() && !FieldModifier.isTransient(field.getModifiers()) && !"this$0".equals(field.getName())) {
                        headBuilder.append(",").append(TranslatorUtils.formatFieldName(field.getName()))
                                .append(",").append(serializerName);
                    }
                }
                headBuilder.append(")").append(NEW_LINE);

            }else {
                headBuilder.append(TAB).append("KACC_DEFINE_ZHUJIE_SERILARIED_INTRUSIVE(")
                        .append(TranslatorUtils.formatClassName(javaClass.getClassName()));
                for (JavaSootField field : fields) {
                    boolean isIgnore = false;
                    String serializerName = "Not_Exist_Serializer";
                    for (AnnotationUsage annotation : field.getAnnotations()) {
                        if (annotation.getAnnotation().getClassName().equals("JsonIgnore")){
                            isIgnore = true;
                            break;
                        }else if (annotation.getAnnotation().getClassName().equals("JsonSerialize")){
                            Map<String, Object> values = annotation.getValues();
                            for (Map.Entry<String, Object> stringObjectEntry : values.entrySet()) {
                                String key = stringObjectEntry.getKey();
                                if ("using".equals(key)){
                                    Object value = stringObjectEntry.getValue();
                                    String className = ((ClassConstant) value).getValue();
                                    serializerName = TranslatorUtils.formatClassName(TranslatorUtils.parseSignature(className));
                                }
                            }
                        }
                    }
                    if (!field.isStatic() && !FieldModifier.isTransient(field.getModifiers()) && !"this$0".equals(field.getName()) && !isIgnore) {
                        headBuilder.append(",").append(TranslatorUtils.formatFieldName(field.getName()))
                                .append(",").append(serializerName);
                    }
                }
                headBuilder.append(")").append(NEW_LINE);
            }


            staticInitMethod.ifPresent(sootMethod ->
                    headBuilder.append(printStaticInitMethod(sootMethod)).append(NEW_LINE));

            if (!initMethods.isEmpty()) {
                boolean isRequireDefaultInit = true;
                for (JavaSootMethod method : initMethods) {
                    if (method.getParameterCount() == 0) {
                        isRequireDefaultInit = false;
                    }
                    headBuilder.append(javaClass.getType().printDeclareMethod(method));
                }
                if (isRequireDefaultInit) {
                    headBuilder.append(TAB)
                            .append(TranslatorUtils.formatClassName(javaClass.getClassName()))
                            .append("() = default;")
                            .append(NEW_LINE);
                }
                headBuilder.append(NEW_LINE);
            }

            if (javaClass.isJsonConstructor()) {
                printConstructorFromJsonDeclare(headBuilder, javaClass);
            }

            // add destructor function
            printDestructorDeclare(headBuilder, javaClass);

            if (!publicMethods.isEmpty()) {
                {
                    for (JavaSootMethod method : publicMethods) {
                        headBuilder.append(javaClass.getType().printDeclareMethod(method));
                    }
                    headBuilder.append(NEW_LINE);
                }
            }

            if (!fields.isEmpty()) {
                for (JavaSootField field : fields) {
                    if (field.getType() instanceof PrimitiveType && field.isStatic() && field.isFinal()) {
                        continue;
                    }
                    Iterable<AnnotationUsage> annotations = field.getAnnotations();
                    if (annotations.iterator().hasNext()) {
                        System.out.println(annotations.iterator().next().toString());
                    }
                    if (field.getType() instanceof JavaClassType &&
                            ((JavaClassType) field.getType()).getFullyQualifiedName().equals(TranslatorContext.getMainClass())) {
                        continue;
                    }
                    headBuilder.append(printField(field));
                }
                headBuilder.append(NEW_LINE);
            }

            if (!javaClass.isAbstract() && NoneUDF.INSTANCE.equals(javaClass.getType())) {
                headBuilder.append(TAB).append("Class * getObjectClass() override{return clazz_;};").append(NEW_LINE);
                headBuilder.append(TAB).append("static Class* getClass();").append(NEW_LINE)
                        .append(TAB).append("static Class* clazz_;").append(NEW_LINE);
            }

            if (!methods.isEmpty()) {
                for (JavaSootMethod method : methods) {
                    headBuilder.append(javaClass.getType().printDeclareMethod(method));
                }
                headBuilder.append(NEW_LINE);
            }

            headBuilder.append("};").append(NEW_LINE);
        }

        // print end micro
        headBuilder.append(NEW_LINE).append("#endif").append(NEW_LINE);
        translateRes.add(headBuilder.toString());
        cppBuilder.append("#include \"").append(TranslatorUtils.formatClassName(javaClass.getClassName()))
                .append(".h\"").append(NEW_LINE);

        if (!javaClass.isAbstract() && NoneUDF.INSTANCE.equals(javaClass.getType())) {
            cppBuilder.append("#include \"").append("basictypes/ClassRegistry.h\"").append(NEW_LINE)
                    .append("#include \"").append("basictypes/ReflectMacros.h\"").append(NEW_LINE);
        }

        if (!javaClass.isLambda()) {
            cppBuilder.append(NEW_LINE).append(printStaticFieldInit(javaClass));

            if (staticInitMethod.isPresent()) {
                cppBuilder.append(NEW_LINE).append(printStaticInit(javaClass));
            }

            staticInitMethod.ifPresent(allNeedTranslateMethod::remove);

            if (!initMethods.isEmpty()) {
                for (JavaSootMethod method : initMethods) {
                    cppBuilder.append(printMethod(javaClass.getType(), method, javaClass.isLambda()))
                            .append(NEW_LINE);
                    allNeedTranslateMethod.remove(method);
                }
            }

            printDestructor(cppBuilder, javaClass);
        }

        if (javaClass.isJsonConstructor()) {
            printConstructorFromJson(cppBuilder, javaClass);
        }

        if (javaClass.getKind().equals(STATIC_METHOD_REF)) {
            printStaticMethodRefUdf(cppBuilder, javaClass);
        }

        if (javaClass.getKind().equals(INSTANCE_METHOD_REF)) {
            printInstanceMethodRefUdf(cppBuilder, javaClass);
        }
        for (JavaSootMethod method : allNeedTranslateMethod) {
            if (method.isAbstract() || TranslatorContext.STATIC_INIT_FUNCTION_NAME.equals(method.getName())) {
                continue;
            }

            cppBuilder.append(printMethod(javaClass.getType(), method, javaClass.isLambda()));
        }

        if (!javaClass.isAbstract() && NoneUDF.INSTANCE.equals(javaClass.getType())) {
            cppBuilder.append("DEFINE_REFLECT_CLASS_BEGIN(")
                    .append(TranslatorUtils.formatClassName(javaClass.getClassName()))
                    .append(")").append(NEW_LINE);

            JavaSootClass javaSootClass = javaClass.getJavaSootClass();
            JavaSootClassSource classSource = javaSootClass.getClassSource();
            List<FieldNode> fieldNodes = null;
            try {
                Class<?> aClass = ClassLoader.getSystemClassLoader()
                        .loadClass("sootup.java.bytecode.frontend.conversion.AsmClassSource");
                Field classNodeField = aClass.getDeclaredField("classNode");
                classNodeField.setAccessible(true);
                ClassNode classNode = null;
                if (classNodeField.get(classSource) instanceof ClassNode) {
                    classNode = (ClassNode) (classNodeField.get(classSource));
                }
                fieldNodes = classNode.fields;
            } catch (Exception e) {
                LOGGER.error("can not get classNode");
            }

            cppBuilder.append(TAB).append("DEFINE_REFLECT_PARENT_CLASS(")
                    .append(TranslatorUtils.formatClassName(javaSootClass.getSuperclass().get().getFullyQualifiedName()))
                    .append(");").append(NEW_LINE);

            for (JavaSootField javaSootField : fields) {
                FieldNode fieldNode = null;
                for (FieldNode field : fieldNodes) {
                    if (field.name.equals(javaSootField.getName())) {
                        fieldNode = field;
                    }
                }

                if (fieldNode == null) {
                    throw new TranslatorException("can not find " + javaSootField.getName() + " field");
                }

                cppBuilder.append(TAB);
                if (javaSootField.getType() instanceof ClassType) {
                    cppBuilder.append("REGISTER_PTR_FIELD(")
                            .append(TranslatorUtils.formatClassName(javaClass.getClassName())).append(", ")
                            .append(TranslatorUtils.formatFieldName(javaSootField.getName())).append(", ")
                            .append(TranslatorUtils.formatParamType(javaSootField.getType())).append(", ").append("\"")
                            .append(TranslatorUtils.parseSignature(
                                            fieldNode.signature == null ? fieldNode.desc : fieldNode.signature)
                                    .replace('.', '_').replace('$', '_')).append("\"")
                            .append(")");
                } else if (javaSootField.getType() instanceof PrimitiveType) {
                    if (javaSootField.isStatic() && javaSootField.isFinal()) {
                        cppBuilder.append(NEW_LINE);
                        continue;
                    }
                    if (!TranslatorContext.getPrimitiveTypeStringMap().containsKey(javaSootField.getType())) {
                        throw new TranslatorException("no support "
                                + ((PrimitiveType) javaSootField.getType()).getName()
                                + " primitive type");
                    }
                    String type = TranslatorContext.getPrimitiveTypeStringMap().get(javaSootField.getType());
                    cppBuilder.append("REGISTER_PRIMITIVE_FIELD(")
                            .append(TranslatorUtils.formatClassName(javaClass.getClassName())).append(", ")
                            .append(TranslatorUtils.formatFieldName(javaSootField.getName())).append(", ")
                            .append(type).append(", ").append("\"")
                            .append(TranslatorUtils.parseSignature(
                                            fieldNode.signature == null ? fieldNode.desc : fieldNode.signature)
                                    .replace('.', '_').replace('$', '_')).append("\"")
                            .append(")");
                } else {
                    throw new TranslatorException("no support type");
                }
                cppBuilder.append(NEW_LINE);
            }
            cppBuilder.append("DEFINE_REFLECT_CLASS_END(")
                    .append(TranslatorUtils.formatClassName(javaClass.getClassName()))
                    .append(")").append(NEW_LINE);
        }
        translateRes.add(cppBuilder.toString());
        return translateRes;
    }

    private static void printStaticMethodRefUdf(StringBuilder cppBuilder, JavaClass javaClass) {
        MethodSignature refMethod = javaClass.getRefMethod();

        cppBuilder.append(javaClass.getType()
                .printMethodRefHeadAndParams(javaClass.getClassName(), refMethod.getParameterTypes()));
        cppBuilder.append(TAB);
        if (!(refMethod.getType() instanceof VoidType) && !(refMethod.getType() instanceof PrimitiveType)) {
            cppBuilder.append("Object *tmp = ");
        }
        StringJoiner params = new StringJoiner(", ");
        for (int i = 0; i < refMethod.getParameterCount(); i++) {
            params.add("in" + i);
        }
        cppBuilder.append(String.format(STATIC_METHOD_INVOKE,
                TranslatorUtils.formatClassName(refMethod.getDeclClassType().getFullyQualifiedName()),
                refMethod.getName(), params));

        int refCount = TranslatorContext.getRefCount(refMethod);
        printMethodRefUdfReturn(cppBuilder, javaClass, refCount);
    }

    private static void printInstanceMethodRefUdf(StringBuilder cppBuilder, JavaClass javaClass) {
        MethodSignature refMethod = javaClass.getRefMethod();

        cppBuilder.append(javaClass.getType()
                .printMethodRefHeadAndParams(javaClass.getClassName(), ImmutableList.of(refMethod.getDeclClassType())));
        cppBuilder.append(TAB);

        if ("<init>".equals(refMethod.getName())) {
            StringJoiner params = new StringJoiner(", ");
            for (int i = 0; i < refMethod.getParameterCount(); i++) {
                params.add("in" + i);
            }
            cppBuilder.append(TranslatorUtils.formatParamType(refMethod.getDeclClassType())).append("tmp = ")
                    .append(String.format(NEW_OBJ,
                            TranslatorUtils.formatClassName(refMethod.getDeclClassType().getFullyQualifiedName()),
                            params))
                    .append(";");
        } else {
            if (!(refMethod.getType() instanceof VoidType) && !(refMethod.getType() instanceof PrimitiveType)) {
                cppBuilder.append(TranslatorUtils.formatParamType(refMethod.getType())).append("tmp = ");
            }
            cppBuilder.append(String.format(INSTANCE_METHOD_INVOKE,
                    "in0",
                    refMethod.getName()));
        }

        int refCount = TranslatorContext.getRefCount(refMethod);
        printMethodRefUdfReturn(cppBuilder, javaClass, refCount);
    }

    private static void printMethodRefUdfReturn(StringBuilder cppBuilder, JavaClass javaClass, int refCount) {
        if (javaClass.getType().refLambdaReturn()) {
            if (refCount == 0) {
                cppBuilder.append(String.format(TranslatorContext.GET_REF, "tmp"));
            }
            cppBuilder.append(TAB).append("return tmp;");
        }
        if (!javaClass.getType().refLambdaReturn()) {
            if (refCount == 1) {
                cppBuilder.append(String.format(TranslatorContext.UNKNOWN_PUT_REF, "tmp"));
            }
            cppBuilder.append(TAB).append("return;");
        }
        cppBuilder.append(NEW_LINE).append("}").append(NEW_LINE);
    }

    private static String printField(JavaSootField field) {
        StringBuilder fieldBuilder = new StringBuilder(TAB);

        if (field.isStatic()) {
            fieldBuilder.append("thread_local static ");
        }

        if (field.getModifiers().contains(FieldModifier.VOLATILE)) {
            fieldBuilder.append("volatile ");
        }

        fieldBuilder.append(TranslatorUtils.formatParamType(field.getType()))
                .append(TranslatorUtils.formatFieldName(field.getName()));

        if (TranslatorUtils.isPrimaryType(field.getType()) || field.isStatic()) {
            fieldBuilder.append(";");
        } else {
            fieldBuilder.append(" = nullptr;");
        }

        return fieldBuilder.append(NEW_LINE).toString();
    }

    private static String printMethod(UDFType type, JavaSootMethod method, boolean isLambda) {
        try {
            return JavaMethodTranslator.translateMethod(type, method, isLambda) + NEW_LINE;
        } catch (TranslatorException e) {
            LOGGER.error("Translate method {} failed, {}", method.getSignature(), e.getMessage());
            LOGGER.error("The translated class has failed methods!");
            throw e;
        }
    }

    private static void printDestructorDeclare(StringBuilder classBuilder, JavaClass javaClass) {
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        classBuilder.append(TAB).append("~").append(className).append("();").append(NEW_LINE).append(NEW_LINE);
    }

    private static void printDestructor(StringBuilder classBuilder, JavaClass javaClass) {
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        classBuilder.append(className).append("::").append("~").append(className).append("() {").append(NEW_LINE);

        for (JavaSootField field : javaClass.getFields()) {
            if (!TranslatorUtils.isPrimaryType(field.getType()) && !field.isStatic()) {
                String fieldName = TranslatorUtils.formatFieldName(field.getName());
                if ("this_0".equals(fieldName)) {
                    continue;
                }

                classBuilder.append(TAB).append("if (").append(fieldName).append(") {")
                        .append(NEW_LINE)
                        .append(TAB).append(TAB).append(fieldName).append("->putRefCount();")
                        .append(NEW_LINE)
                        .append(TAB).append("}").append(NEW_LINE);
            }
        }

        classBuilder.append("}").append(NEW_LINE).append(NEW_LINE);
    }

    private static void printConstructorFromJsonDeclare(StringBuilder classBuilder, JavaClass javaClass) {
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        StringBuilder declareBuilder = new StringBuilder(TAB)
                .append(className)
                .append("(nlohmann::json jsonObj);");

        classBuilder.append(declareBuilder).append(NEW_LINE);
    }

    private static String printConstructorFromJson(StringBuilder methodBuilder, JavaClass javaClass) {
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        methodBuilder.append(className)
                .append("::")
                .append(className)
                .append("(nlohmann::json jsonObj)");

        // deal with father class......should deal with extends class
        Set<ClassType> superClassForJson = javaClass.getSupperClasses().stream()
                .filter(c -> !TranslatorContext.getIgnoredClasses().contains(c.getFullyQualifiedName()))
                .filter(c -> !TranslatorContext.getStringMap().containsKey(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (!superClassForJson.isEmpty()) {
            methodBuilder.append(" : ");
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (ClassType classType : superClassForJson) {
            String fatherName = TranslatorUtils.formatClassName(classType.getFullyQualifiedName());
            joiner.add(fatherName + "(jsonObj)");
        }

        methodBuilder.append(joiner).append(NEW_LINE).append(" {").append(NEW_LINE);

        // fill fields
        for (JavaSootField field : javaClass.getFields()) {
            if (field.getType() instanceof ClassType &&
                    ((ClassType) field.getType()).getFullyQualifiedName().equals(TranslatorContext.getMainClass())) {
                continue;
            }
            if (!field.isStatic() && !FieldModifier.isTransient(field.getModifiers())) {
                methodBuilder.append(TAB)
                        .append("if(!jsonObj[\"")
                        .append(TranslatorUtils.formatFieldName(field.getName()))
                        .append("\"].empty()) {")
                        .append(NEW_LINE);
                if (field.getType() instanceof PrimitiveType) {
                    methodBuilder.append(TAB).append(TAB)
                            .append("this->")
                            .append(TranslatorUtils.formatFieldName(field.getName()))
                            .append(" = jsonObj[\"")
                            .append(TranslatorUtils.formatFieldName(field.getName()))
                            .append("\"];")
                            .append(NEW_LINE);
                } else {
                    methodBuilder.append(TAB).append(TAB)
                            .append("this->")
                            .append(TranslatorUtils.formatFieldName(field.getName()))
                            .append(" = new ")
                            .append(TranslatorUtils.formatType(field.getType()))
                            .append("(jsonObj[\"")
                            .append(TranslatorUtils.formatFieldName(field.getName()))
                            .append("\"]);")
                            .append(NEW_LINE);
                }
                methodBuilder.append(TAB).append("}").append(NEW_LINE);
            }
        }

        methodBuilder.append("}").append(NEW_LINE).append(NEW_LINE);

        return methodBuilder.toString();
    }

    private static String printStaticFieldInit(JavaClass javaClass) {
        StringBuilder initBuilder = new StringBuilder();
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        for (JavaSootField sootField : javaClass.getFields()) {
            if (sootField.isStatic() && !(sootField.getType() instanceof PrimitiveType)) {
                initBuilder.append("thread_local ")
                        .append(TranslatorUtils.formatParamType(sootField.getType()))
                        .append(className)
                        .append("::")
                        .append(TranslatorUtils.formatFieldName(sootField.getName()))
                        .append(" = nullptr;")
                        .append(NEW_LINE);
            }
        }

        return initBuilder.toString();
    }

    private static String printStaticInitMethod(JavaSootMethod method) {
        String body = JavaMethodTranslator.translateMethod(NoneUDF.INSTANCE, method, false);

        return TAB + "static int " + STATIC_INIT_METHOD + "() {" + NEW_LINE + body + TAB + "}" + NEW_LINE + NEW_LINE
                + TAB + "thread_local static int " + STATIC_INIT_FIELD + ";" + NEW_LINE;
    }

    private static String printStaticInit(JavaClass javaClass) {
        String clzName = TranslatorUtils.formatClassName(javaClass.getClassName());

        return "thread_local int " + clzName + "::" + STATIC_INIT_FIELD + " = "
                + clzName + "::" + STATIC_INIT_METHOD + "();" + NEW_LINE;
    }
}
