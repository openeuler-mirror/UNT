/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type.flink;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;

import com.google.common.collect.ImmutableSet;

import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import org.apache.flink.api.common.functions.ReduceFunction;

import java.util.Set;
import java.util.List;

/**
 * Flink ReduceFunction
 *
 * @since 2025-05-19
 */
public class FlinkReduceFunction implements UDFType {
    /**
     * Flink Reduce Function instance
     */
    public static final FlinkReduceFunction INSTANCE = new FlinkReduceFunction();

    @Override
    public Class<?> getBaseClass() {
        return ReduceFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n"
                + "extern \"C\" std::unique_ptr<ReduceFunction<Object>> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
    }

    @Override
    public String getSoPrefix() {
        return "libreduce";
    }


    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!method.getName().equals("reduce")) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        return method.getParameterCount() == 2
                && method.getReturnType() instanceof ClassType
                && method.getParameterType(0) instanceof ClassType
                && method.getParameterType(1) instanceof ClassType;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (method.getName().equals("reduce")
                && isUdfFunction(method)) {
            return "    Object *reduce(Object *input0, Object *input1) override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclClassType());

        if (methodContext.getJavaMethod().getName().equals("reduce")
                && isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder()
                    .append("Object *")
                    .append(className)
                    .append("::")
                    .append("reduce(Object *input0, Object *input1)")
                    .append(NEW_LINE)
                    .append("{")
                    .append(NEW_LINE);

            if (methodContext.isIgnore()) {
                return headBuilder.toString();
            }

            for (int i = 0; i < methodContext.getParams().size(); i++) {
                Local paramLocal = methodContext.getParams().get(i);
                methodContext.removeLocal(paramLocal);

                String typeString = TranslatorUtils.formatType(paramLocal.getType());

                headBuilder.append(TAB).append(TAB)
                        .append(typeString).append(" *").append(TranslatorUtils.formatLocalName((paramLocal)))
                        .append(" = reinterpret_cast<").append(typeString).append(" *>(input").append(i).append(");")
                        .append(NEW_LINE);
            }

            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public String printLambdaDeclare() {
        return "    Object *reduce(Object *input0, Object *input1)  override;" + NEW_LINE;
    }

    @Override
    public String printLambdaHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatClassName(
                TranslatorUtils.formatLambdaUdfClassName(
                        methodContext.getJavaMethod().getSignature(),
                        methodContext.getUdfType())
        );

        StringBuilder headBuilder = new StringBuilder()
                .append("Object *")
                .append(TranslatorUtils.formatClassName(className))
                .append("::")
                .append("reduce(Object *input0, Object *input1)")
                .append(NEW_LINE)
                .append("{")
                .append(NEW_LINE);

        for (int i = 0; i < methodContext.getParams().size(); i++) {
            Local paramLocal = methodContext.getParams().get(i);
            methodContext.removeLocal(paramLocal);

            String typeString = TranslatorUtils.formatType(paramLocal.getType());

            headBuilder.append(TAB)
                    .append(typeString).append(" *").append(TranslatorUtils.formatLocalName((paramLocal)))
                    .append(" = reinterpret_cast<").append(typeString).append(" *>(input").append(i).append(");")
                    .append(NEW_LINE);
        }

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public String printMethodRefHeadAndParams(String className, List<Type> paramTypes) {
        StringBuilder headBuilder = new StringBuilder()
                .append("Object *")
                .append(className)
                .append("::")
                .append("reduce(Object *input0, Object *input1)")
                .append(NEW_LINE)
                .append("{")
                .append(NEW_LINE);

        for (int i = 0; i < 2; i++) {
            String typeString = TranslatorUtils.formatType(paramTypes.get(i));

            headBuilder.append(TAB)
                    .append(typeString).append(" *").append("in").append(i)
                    .append(" = reinterpret_cast<").append(typeString).append(" *>(input").append(i).append(");")
                    .append(NEW_LINE);
        }

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public boolean refLambdaReturn() {
        return true;
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(ReduceFunction.class.getName()));
    }
}
