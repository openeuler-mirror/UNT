package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.translator.visitor.TranslatorTypeVisitor;
import com.huawei.unt.type.UDFType;
import org.apache.flink.api.java.functions.KeySelector;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;

public class FlinkKeySelector implements UDFType {
    public static final FlinkKeySelector INSTANCE = new FlinkKeySelector();

    @Override
    public Class<?> getBaseClass() {
        return KeySelector.class;
    }

    @Override
    public String getCppFileString(String className) {

        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<KeySelector> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";

//        return "#include \"" + className + ".h\"\n\n" +
//                "extern \"C\" size_t Hash(Object *value) {\n" +
//                "    return " + className + "::Hash(value);\n"+
//                "}\n\n" +
//                "extern \"C\" bool Cmp(Object *value1, Object *value2) {\n" +
//                "    return " + className + "::Cmp(value1, value2);\n" +
//                "}\n";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!method.getName().equals("getKey")) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        return method.getParameterCount() == 1 &&
                method.getReturnType() instanceof ClassType &&
                method.getParameterType(0) instanceof ClassType;
    }

    @Override
    public String getSoPrefix() {
        return "libkeyby";
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (method.getName().equals("getKey") && isUdfFunction(method)) {
            return "    Object *getKey(Object *obj) override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclClassType());

        if (methodContext.getJavaMethod().getName().equals("getKey") &&
                isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder();

            headBuilder.append("Object *")
                    .append(className)
                    .append("::")
                    .append("getKey(Object *obj) {")
                    .append(NEW_LINE);

            Local paramLocal = methodContext.getParams().get(0);
            methodContext.removeLocal(paramLocal);

            String typeString = TranslatorUtils.formatType(paramLocal.getType());

            headBuilder.append(TranslatorContext.TAB)
                    .append(typeString).append(" *").append(TranslatorUtils.formatLocalName(paramLocal))
                    .append(" = reinterpret_cast<").append(typeString).append(" *>(obj);")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public String printLambdaDeclare() {
        return "    Object *getKey(Object *obj) override;" + NEW_LINE;
    }

    @Override
    public String printLambdaHeadAndParams(MethodContext methodContext) {
        String declClassName = TranslatorUtils.formatClassName(
                methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());
        String methodName = TranslatorUtils.formatClassName(methodContext.getJavaMethod().getName());
        String className = declClassName + "_" + methodName;

        StringBuilder headBuilder = new StringBuilder();

        headBuilder.append("Object *")
                .append(className)
                .append("::")
                .append("getKey(Object *obj) {")
                .append(NEW_LINE);

        Local paramLocal = methodContext.getParams().get(0);
        methodContext.removeLocal(paramLocal);

        String typeString = TranslatorUtils.formatType(paramLocal.getType());

        headBuilder.append(TranslatorContext.TAB)
                .append(typeString).append(" *").append(TranslatorUtils.formatLocalName(paramLocal))
                .append(" = reinterpret_cast<").append(typeString).append(" *>(obj);")
                .append(NEW_LINE);

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(KeySelector.class.getName()));
    }

    public String getDeclareHashAndCmpFunction(JavaClass javaClass) {
        String className = TranslatorUtils.formatClassName(javaClass.getClassName());

        return NEW_LINE + "public:" + NEW_LINE +
                "    static " + className + "* instance;" + NEW_LINE +
                "    static size_t Hash(Object *obj);" + NEW_LINE +
                "    static bool Cmp(Object *value1, Object *value2);" + NEW_LINE;
    }

    public String getHashAndCmpFunction(JavaClass javaClass) {
        String className;
        Type keyType = null;

        if (javaClass.isLambda()) {
            JavaSootMethod method = javaClass.getMethods().stream().findFirst().get();

            String declClassName = TranslatorUtils.formatClassName(
                    method.getDeclClassType().getFullyQualifiedName());
            String methodName = TranslatorUtils.formatClassName(method.getName());
            className = declClassName + "_" + methodName;
            keyType = method.getReturnType();
        } else {
            className = TranslatorUtils.formatClassName(javaClass.getClassName());
            for (JavaSootMethod method : javaClass.getMethods()) {
                if (isUdfFunction(method)) {
                    keyType = method.getReturnType();
                    break;
                }
            }

            if (!(keyType instanceof ClassType)) {
                throw  new TranslatorException("key selector type is empty or not class type.");
            }
        }

        String keyTypeName = TranslatorTypeVisitor.getTypeString(keyType);

        // print instance
        return NEW_LINE + className + " *" + className + "::instance = new " + className + "();" + NEW_LINE + NEW_LINE +
                // print Hash
                "size_t " + className + "::Hash(Object *obj) {" +NEW_LINE +
                "    " + keyTypeName + " *r1 = reinterpret_cast<" + keyTypeName + " *>(" +
                className + "::instance->getKey(obj));" + NEW_LINE +
                "    return r1->hashCode();" + NEW_LINE +
                "}" + NEW_LINE + NEW_LINE +
                // print Cmp
                "bool " + className + "::Cmp(Object *value1, Object *value2) {" + NEW_LINE +
                "    " + keyTypeName + " *r1 = reinterpret_cast<" + keyTypeName + " *>(" +
                className + "::instance->getKey(value1));" + NEW_LINE +
                "    Object *r2 = " + className + "::instance->getKey(value2);" + NEW_LINE +
                "    return r1->equals(r2);" + NEW_LINE +
                "}" + NEW_LINE;
    }
}
