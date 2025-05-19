package com.huawei.unt.translator;

import com.huawei.unt.model.JavaClass;
import com.huawei.unt.type.NoneUDF;
import com.huawei.unt.type.UDFType;
import com.huawei.unt.type.flink.FlinkKeySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.model.FieldModifier;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class JavaClassTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaClassTranslator.class);
    private static final String ACCESS_PREFIX = "access$";
    private static final String STATIC_INIT_FIELD = "initStaticField";
    private static final String STATIC_INIT_METHOD = "initStatic";

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

        fields = fields.stream().filter(f -> !(f.getType() instanceof ClassType &&
            TranslatorContext.IGNORED_CLASSES.contains(((ClassType) f.getType()).getFullyQualifiedName())
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

        //dealWith loopIncludes
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
                .filter(c -> !TranslatorContext.IGNORED_CLASSES.contains(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (!superClasses.isEmpty()) {
            headBuilder.append(" : ");
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (ClassType superClassType : superClasses) {
            if (!TranslatorContext.IGNORED_CLASSES.contains(superClassType.getFullyQualifiedName())) {
                joiner.add("public " + TranslatorUtils.formatType(superClassType));
            }
        }

        headBuilder.append(joiner).append(" {").append(NEW_LINE);
        Set<JavaSootMethod> allNeedTranslateMethod = new HashSet<>(javaClass.getMethods());

        if (javaClass.isLambda()) {
            headBuilder.append("public:").append(NEW_LINE);
            headBuilder.append(javaClass.getType().printLambdaDeclare());
            if (javaClass.isJsonConstructor()){
                printConstructorFromJsonDeclare(headBuilder, javaClass);
            }
//            if (javaClass.getType() instanceof FlinkKeySelector) {
//                headBuilder.append(((FlinkKeySelector) javaClass.getType()).getDeclareHashAndCmpFunction(javaClass));
//            }

            headBuilder.append("};").append(NEW_LINE);
        } else {

            // all public/protected elements into public temp, if function start with access$ change it to public
            List<JavaSootMethod> publicMethods = methods.stream()
                    .filter(m -> m.isPublic() || m.isProtected() || m.getName().startsWith(ACCESS_PREFIX))
                    .collect(Collectors.toList());

            List<JavaSootField> publicFields = fields.stream()
                    .filter(f -> (f.isPublic() || f.isProtected() || f.getName().startsWith(ACCESS_PREFIX)))
                    .collect(Collectors.toList());

            publicFields.forEach(fields::remove);
            publicMethods.forEach(methods::remove);

            headBuilder.append("public:").append(NEW_LINE);

            staticInitMethod.ifPresent(sootMethod ->
                    headBuilder.append(printStaticInitMethod(sootMethod)).append(NEW_LINE));

            if (!initMethods.isEmpty()) {
                for (JavaSootMethod method : initMethods) {
                    headBuilder.append(javaClass.getType().printDeclareMethod(method));
                }
                headBuilder.append(NEW_LINE);
            }

            if (javaClass.isJsonConstructor()){
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

            if (!publicFields.isEmpty()) {
                for (JavaSootField field : publicFields) {
                    headBuilder.append(printField(field));
                }
                headBuilder.append(NEW_LINE);
            }

//            if (javaClass.getType() instanceof FlinkKeySelector) {
//                headBuilder.append(((FlinkKeySelector) javaClass.getType()).getDeclareHashAndCmpFunction(javaClass));
//            }

            if (!fields.isEmpty() || !methods.isEmpty()) {
                headBuilder.append("private:").append(NEW_LINE);
            }

            if (!methods.isEmpty()) {
                for (JavaSootMethod method : methods) {
                    headBuilder.append(javaClass.getType().printDeclareMethod(method));
                }
                headBuilder.append(NEW_LINE);
            }

            if (!fields.isEmpty()) {
                for (JavaSootField field : fields) {
                    headBuilder.append(printField(field));
                }
                headBuilder.append(NEW_LINE);
            }

            headBuilder.append("};").append(NEW_LINE);

//            if (staticInitMethod.isPresent()) {
//                classBuilder.append(NEW_LINE).append(printStaticInit(javaClass));
//            }
//
//            staticInitMethod.ifPresent(allNeedTranslateMethod::remove);
//
//            if (!initMethods.isEmpty()) {
//                for (JavaSootMethod method : initMethods) {
//                    classBuilder.append(printMethod(javaClass.getType(), method, javaClass.isLambda()))
//                            .append(NEW_LINE);
//                    allNeedTranslateMethod.remove(method);
//                }
//            }
//
//            if (javaClass.isJsonConstructor()){
//                printConstructorFromJson(classBuilder, javaClass);
//            }
//
//            printDestructor(classBuilder, javaClass);
        }

        // print end micro
        headBuilder.append(NEW_LINE).append("#endif").append(NEW_LINE);

        translateRes.add(headBuilder.toString());

        cppBuilder.append("#include \"").append(TranslatorUtils.formatClassName(javaClass.getClassName())).append(".h\"").append(NEW_LINE);
        if (!javaClass.isLambda()){

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

        if (javaClass.isJsonConstructor()){
            printConstructorFromJson(cppBuilder, javaClass);
        }

        for (JavaSootMethod method : allNeedTranslateMethod) {
            if (method.isAbstract() || TranslatorContext.STATIC_INIT_FUNCTION_NAME.equals(method.getName())) {
                continue;
            }

            cppBuilder.append(printMethod(javaClass.getType(), method, javaClass.isLambda()));
        }

//        if (javaClass.getType() instanceof FlinkKeySelector) {
//            cppBuilder.append(((FlinkKeySelector) javaClass.getType()).getHashAndCmpFunction(javaClass));
//        }

        // print end micro
//        classBuilder.append(NEW_LINE).append("#endif").append(NEW_LINE);

        translateRes.add(cppBuilder.toString());

        return translateRes;
    }

    private static String printField(JavaSootField field) {
        StringBuilder fieldBuilder = new StringBuilder(TAB);

        if (field.isStatic()) {
            fieldBuilder.append("thread_local static ");
        }

        if (field.getModifiers().contains(FieldModifier.VOLATILE)){
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
            LOGGER.warn("The translated class has failed methods!");
            TranslatorContext.IGNORED_METHODS.add(method.getSignature().toString());
            return JavaMethodTranslator.translateMethod(type, method, isLambda) + NEW_LINE;
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
                if (fieldName.equals("this_0")) continue;

                classBuilder.append(TAB) .append("if (").append(fieldName).append(") {")
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
                .filter(c -> !TranslatorContext.IGNORED_CLASSES.contains(c.getFullyQualifiedName()))
                .filter(c -> !TranslatorContext.CLASS_MAP.containsKey(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (!superClassForJson.isEmpty()){
            methodBuilder.append(" : ");
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (ClassType classType : superClassForJson) {
            String fatherName = TranslatorUtils.formatClassName(classType.getFullyQualifiedName());
            joiner.add(fatherName + "(jsonObj)");
        }

        methodBuilder.append(joiner).append(NEW_LINE).append(" {").append(NEW_LINE);

//        if (!javaClass.getSupperClasses().isEmpty()) {
//            ClassType classType = javaClass.getSupperClasses().stream().findAny().get();
//            String fatherName = TranslatorUtils.formatClassName(classType.getFullyQualifiedName());
//            methodBuilder.append(" : ")
//                    .append(fatherName)
//                    .append("(jsonObj)")
//                    .append(NEW_LINE)
//                    .append("{")
//                    .append(NEW_LINE);
//        }

        // fill fields
        for (JavaSootField field : javaClass.getFields()) {
            if (!field.isStatic()) {
                methodBuilder.append(TAB)
                        .append("if(!jsonObj[\"")
                        .append(TranslatorUtils.formatFieldName(field.getName()))
                        .append("\"].empty()){")
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
                        .append(sootField.getName())
                        .append(" = nullptr;")
                        .append(NEW_LINE);
            }
        }

        return initBuilder.toString();
    }

    private static String printStaticInitMethod(JavaSootMethod method) {
        String body = JavaMethodTranslator.translateMethod(NoneUDF.INSTANCE, method, false);

        return TAB + "static int " + STATIC_INIT_METHOD + "() {" + NEW_LINE +
                body + TAB + "}" + NEW_LINE + NEW_LINE +
                TAB + "thread_local static int " + STATIC_INIT_FIELD + ";" + NEW_LINE;
    }

    private static String printStaticInit(JavaClass javaClass) {
        String clzName = TranslatorUtils.formatClassName(javaClass.getClassName());

        return "thread_local int " + clzName + "::" + STATIC_INIT_FIELD + " = " +
                clzName + "::" + STATIC_INIT_METHOD + "();" + NEW_LINE;
    }

    private JavaClassTranslator() {}
}
