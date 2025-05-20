package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.configuration.Configuration;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class FlinkReduceFunction implements UDFType {
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

        if (method.getName().equals("reduce")) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        return method.getParameterCount() == 1;
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
        String declClassName = TranslatorUtils.formatClassName(
                methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());
        String methodName = TranslatorUtils.formatClassName(methodContext.getJavaMethod().getName());
        String className = declClassName + "_" + methodName;

        StringBuilder headBuilder = new StringBuilder()
                .append("Object *")
                .append(className)
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
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(ReduceFunction.class.getName()));
    }
}
