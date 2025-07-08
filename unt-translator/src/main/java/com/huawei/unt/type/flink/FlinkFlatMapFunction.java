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

import java.util.List;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.util.Set;

/**
 * Flink dataStream FlatMapFunction
 *
 * @since 2025-05-19
 */
public class FlinkFlatMapFunction implements UDFType {
    /**
     * Flink flatMapFunction instance
     */
    public static final FlinkFlatMapFunction INSTANCE = new FlinkFlatMapFunction();

    @Override
    public Class<?> getBaseClass() {
        return FlatMapFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<FlatMapFunction<Object>> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
    }

    @Override
    public String getSoPrefix() {
        return "libflatmap";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!ImmutableSet.of("flatMap").contains(method.getName())) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        if (method.getName().equals("flatMap")) {
            return method.getParameterCount() == 2
                    && method.getReturnType() instanceof VoidType
                    && method.getParameterType(1) instanceof ClassType
                    && ((ClassType) method.getParameterType(1)).getClassName().equals("Collector");
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (method.getName().equals("flatMap") && isUdfFunction(method)) {
            return "    void flatMap(Object *obj, Collector *collector) override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        if (methodContext.getJavaMethod().getName().equals("flatMap")
                && isUdfFunction(methodContext.getJavaMethod())) {
            String className = TranslatorUtils.formatClassName(
                    methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());

            StringBuilder headBuilder = new StringBuilder("void ")
                    .append(className)
                    .append("::flatMap(Object *obj, Collector *collector) {")
                    .append(NEW_LINE);

            if (methodContext.isIgnore()) {
                return headBuilder.toString();
            }

            Local param1 = methodContext.getParams().get(0);
            methodContext.removeLocal(param1);
            String typeString = TranslatorUtils.formatType(param1.getType());

            headBuilder.append(TAB)
                    .append(typeString).append(" *")
                    .append(TranslatorUtils.formatLocalName(param1))
                    .append(" = reinterpret_cast<")
                    .append(typeString).append(" *>(obj);")
                    .append(NEW_LINE);

            Local param2 = methodContext.getParams().get(1);
            methodContext.removeLocal(param2);
            headBuilder.append(TAB)
                    .append("Collector *")
                    .append(TranslatorUtils.formatLocalName(param2))
                    .append(" = collector;")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public String printLambdaDeclare() {
        return "    void flatMap(Object *obj, Collector *collector)  override;" + NEW_LINE;
    }

    @Override
    public String printLambdaHeadAndParams(MethodContext methodContext) {
        String declClassName = TranslatorUtils.formatClassName(
                methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());
        String methodName = TranslatorUtils.formatClassName(methodContext.getJavaMethod().getName());
        String className = declClassName + "_" + methodName;

        StringBuilder headBuilder = new StringBuilder("void ")
                .append(className)
                .append("::flatMap(Object *obj, Collector *collector) {")
                .append(NEW_LINE);

        Local param1 = methodContext.getParams().get(0);
        methodContext.removeLocal(param1);
        String typeString = TranslatorUtils.formatType(param1.getType());

        headBuilder.append(TAB)
                .append(typeString).append(" *")
                .append(TranslatorUtils.formatLocalName(param1))
                .append(" = reinterpret_cast<")
                .append(typeString).append(" *>(obj);")
                .append(NEW_LINE);

        Local param2 = methodContext.getParams().get(1);
        methodContext.removeLocal(param2);
        headBuilder.append(TAB)
                .append("Collector *")
                .append(TranslatorUtils.formatLocalName(param2))
                .append(" = collector;")
                .append(NEW_LINE);

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public String printMethodRefHeadAndParams(String className, List<Type> paramTypes) {
        StringBuilder headBuilder = new StringBuilder("void ")
                .append(className)
                .append("::flatMap(Object *obj, Collector *collector) {")
                .append(NEW_LINE);


        String typeString = TranslatorUtils.formatType(paramTypes.get(0));

        headBuilder.append(TAB)
                .append(typeString).append(" *")
                .append("in0")
                .append(" = reinterpret_cast<")
                .append(typeString).append(" *>(obj);")
                .append(NEW_LINE);

        headBuilder.append(TAB)
                .append("Collector *")
                .append("in1")
                .append(" = collector;")
                .append(NEW_LINE);

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public boolean refLambdaReturn() {
        return false;
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(FlatMapFunction.class.getName()),
                factory.getClassType(Collector.class.getName()));
    }
}
