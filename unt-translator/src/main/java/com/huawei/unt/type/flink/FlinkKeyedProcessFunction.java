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
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction;

import java.util.Set;

/**
 * Flink dataStream FlinkKeyedProcessFunction
 *
 * @since 2025-06-30
 */
public class FlinkKeyedProcessFunction implements UDFType {
    /**
     * Flink KeyedProcessFunction instance
     */
    public static final FlinkKeyedProcessFunction INSTANCE = new FlinkKeyedProcessFunction();

    @Override
    public Class<?> getBaseClass() {
        return KeyedProcessFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
        + "extern \"C\" std::unique_ptr<KeyedProcessFunction<Object, Object*, Object*>> "
        + "NewInstance(nlohmann::json jsonObj) {\n"
        + "    return std::make_unique<" + className + ">(jsonObj);\n"
        + "}";
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(KeyedProcessFunction.class.getName()));
    }

    @Override
    public String getSoPrefix() {
        return "libprocess";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!ImmutableSet.of("processElement").contains(method.getName())) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        if ("processElement".equals(method.getName())) {
            return method.getParameterCount() == 3
                    && method.getReturnType() instanceof VoidType
                    && method.getParameterType(0) instanceof ClassType
                    && method.getParameterType(1) instanceof ClassType
                    && "KeyedProcessFunction$Context".equals(((ClassType) method.getParameterType(1)).getClassName())
                    && method.getParameterType(2) instanceof ClassType
                    && "Collector".equals(((ClassType) method.getParameterType(2)).getClassName());
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (isUdfFunction(method) && "processElement".equals(method.getName())) {
            return "    void processElement("
            + "Object *obj, KeyedProcessFunction<Object, Object*, Object*>::Context *ctx, Collector *collector"
            + ") override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        if ("processElement".equals(methodContext.getJavaMethod().getName())
                && isUdfFunction(methodContext.getJavaMethod())) {
            String className = TranslatorUtils.formatClassName(
                    methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());

            StringBuilder headBuilder = new StringBuilder("void ")
                    .append(className)
                    .append("::processElement(")
                    .append("Object *obj, KeyedProcessFunction<Object, Object*, Object*>"
                            + "::Context *ctx, Collector *collector) {")
                    .append(NEW_LINE);

            Local param1 = methodContext.getParams().get(0);
            methodContext.removeLocal(param1);
            String typeString = TranslatorUtils.formatType(param1.getType());

            headBuilder.append(TranslatorContext.TAB)
                    .append(typeString).append(" *")
                    .append(TranslatorUtils.formatLocalName(param1))
                    .append(" = reinterpret_cast<")
                    .append(typeString).append(" *>(obj);")
                    .append(NEW_LINE);

            Local param2 = methodContext.getParams().get(1);
            methodContext.removeLocal(param2);
            headBuilder.append(TranslatorContext.TAB)
                    .append("KeyedProcessFunction<Object, Object*, Object*>::Context *")
                    .append(TranslatorUtils.formatLocalName(param2))
                    .append(" = ctx;")
                    .append(NEW_LINE);

            Local param3 = methodContext.getParams().get(2);
            methodContext.removeLocal(param3);
            headBuilder.append(TranslatorContext.TAB)
                    .append("Collector *")
                    .append(TranslatorUtils.formatLocalName(param3))
                    .append(" = collector;")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public String printLambdaDeclare() {
        return "    void processElement("
        + "Object *obj, KeyedProcessFunction<Object, Object*, Object*>::Context *ctx, Collector *collector"
        + ") override;" + NEW_LINE;
    }

    @Override
    public String printLambdaHeadAndParams(MethodContext methodContext) {
        String declClassName = TranslatorUtils.formatClassName(
                methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());
        String methodName = TranslatorUtils.formatClassName(methodContext.getJavaMethod().getName());
        String className = declClassName + "_" + methodName;

        StringBuilder headBuilder = new StringBuilder("void ")
                .append(className)
                .append("::processElement(")
                .append("Object *obj, KeyedProcessFunction<Object, Object*, Object*>"
                        + "::Context *ctx, Collector *collector) {")
                .append(NEW_LINE);

        Local param1 = methodContext.getParams().get(0);
        methodContext.removeLocal(param1);
        String typeString = TranslatorUtils.formatType(param1.getType());

        headBuilder.append(TranslatorContext.TAB)
                .append(typeString).append(" *")
                .append(TranslatorUtils.formatLocalName(param1))
                .append(" = reinterpret_cast<")
                .append(typeString).append(" *>(obj);")
                .append(NEW_LINE);

        Local param2 = methodContext.getParams().get(1);
        methodContext.removeLocal(param2);
        headBuilder.append(TranslatorContext.TAB)
                .append("KeyedProcessFunction<Object, Object*, Object*>::Context *")
                .append(TranslatorUtils.formatLocalName(param2))
                .append(" = ctx;")
                .append(NEW_LINE);

        Local param3 = methodContext.getParams().get(2);
        methodContext.removeLocal(param3);
        headBuilder.append(TranslatorContext.TAB)
                .append("TimestampedCollector *")
                .append(TranslatorUtils.formatLocalName(param3))
                .append(" = collector;")
                .append(NEW_LINE);

        return headBuilder.append(NEW_LINE).toString();
    }
}
