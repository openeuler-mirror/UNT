/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type.flink;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;

import com.google.common.collect.ImmutableSet;

import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import org.apache.flink.api.java.functions.KeySelector;

import java.util.Set;

/**
 * Flink keySelector
 *
 * @since 2025-05-19
 */
public class FlinkKeySelector implements UDFType {
    /**
     * instance
     */
    public static final FlinkKeySelector INSTANCE = new FlinkKeySelector();

    @Override
    public Class<?> getBaseClass() {
        return KeySelector.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<KeySelect<Object>> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
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

        return method.getParameterCount() == 1
                && method.getReturnType() instanceof ClassType
                && method.getParameterType(0) instanceof ClassType;
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

        if ("getKey".equals(methodContext.getJavaMethod().getName())
                && isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder();

            headBuilder.append("Object *")
                    .append(className)
                    .append("::")
                    .append("getKey(Object *obj) {")
                    .append(NEW_LINE);

            if (methodContext.isIgnore()) {
                return headBuilder.toString();
            }

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
}
