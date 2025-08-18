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

import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Set;

/**
 * Flink dataStream FlinkKeyedCoProcessFunction
 *
 * @since 2025-06-30
 */
public class FlinkKeyedCoProcessFunction implements UDFType {
    /**
     * Flink KeyedCoProcessFunction instance
     */
    public static final FlinkKeyedCoProcessFunction INSTANCE = new FlinkKeyedCoProcessFunction();

    @Override
    public Class<?> getBaseClass() {
        return KeyedCoProcessFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
        + "extern \"C\" std::unique_ptr<KeyedCoProcessFunction<Object, Object*, Object*, Object*>> "
        + "NewInstance(nlohmann::json jsonObj) {\n"
        + "    return std::make_unique<" + className + ">(jsonObj);\n"
        + "}";
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(KeyedCoProcessFunction.class.getName()),
                factory.getClassType(KeyedCoProcessFunction.Context.class.getName()),
                factory.getClassType(Collector.class.getName()));
    }

    @Override
    public String getSoPrefix() {
        return "libcoprocess";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }
        if (!ImmutableSet.of("processElement1", "processElement2").contains(method.getName())) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        if ("processElement1".equals(method.getName()) || "processElement2".equals(method.getName())) {
            return method.getParameterCount() == 3
                    && method.getReturnType() instanceof VoidType
                    && method.getParameterType(0) instanceof ClassType
                    && method.getParameterType(1) instanceof ClassType
                    && "KeyedCoProcessFunction$Context".equals(((ClassType) method.getParameterType(1)).getClassName())
                    && method.getParameterType(2) instanceof ClassType
                    && "Collector".equals(((ClassType) method.getParameterType(2)).getClassName());
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (isUdfFunction(method) && "processElement1".equals(method.getName())) {
            return "    void processElement1(Object *obj, Context *ctx, Collector *collector) override;" + NEW_LINE;
        }
        if (isUdfFunction(method) && "processElement2".equals(method.getName())) {
            return "    void processElement2(Object *obj, Context *ctx, Collector *collector) override;" + NEW_LINE;
        }
        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        if (("processElement1".equals(methodContext.getJavaMethod().getName())
                || "processElement2".equals(methodContext.getJavaMethod().getName()))
                && isUdfFunction(methodContext.getJavaMethod())) {
            String className = TranslatorUtils.formatClassName(
                    methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());

            StringBuilder headBuilder = new StringBuilder("void ")
                    .append(className)
                    .append("::")
                    .append(methodContext.getJavaMethod().getName())
                    .append("(Object *obj, KeyedCoProcessFunction<Object, Object*, Object*, Object*>"
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
                    .append("KeyedCoProcessFunction<Object, Object*, Object*, Object*>::Context *")
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
}
