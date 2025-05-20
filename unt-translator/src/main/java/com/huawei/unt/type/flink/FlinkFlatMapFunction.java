package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.util.Collector;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;

public class FlinkFlatMapFunction implements UDFType {
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

        headBuilder.append(TranslatorContext.TAB)
                .append(typeString).append(" *")
                .append(TranslatorUtils.formatLocalName(param1))
                .append(" = reinterpret_cast<")
                .append(typeString).append(" *>(obj);")
                .append(NEW_LINE);

        Local param2 = methodContext.getParams().get(1);
        methodContext.removeLocal(param2);
        headBuilder.append(TranslatorContext.TAB)
                .append("Collector *")
                .append(TranslatorUtils.formatLocalName(param2))
                .append(" = collector;")
                .append(NEW_LINE);

        return headBuilder.append(NEW_LINE).toString();
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(FlatMapFunction.class.getName()),
                factory.getClassType(Collector.class.getName()));
    }
}
