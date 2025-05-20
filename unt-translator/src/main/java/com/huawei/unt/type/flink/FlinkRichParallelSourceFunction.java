package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import sootup.core.jimple.basic.Local;
import sootup.core.types.ClassType;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class FlinkRichParallelSourceFunction implements UDFType {
    public static final FlinkRichParallelSourceFunction INSTANCE = new FlinkRichParallelSourceFunction();

    @Override
    public Class<?> getBaseClass() {
        return RichParallelSourceFunction.class;
    }

    @Override
    public String getCppFileString(String className) {

        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<RichParallelSourceFunction> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
    }

    @Override
    public String getSoPrefix() {
        return "librichparallelsource";
    }

    // just run and cancel now
    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!ImmutableList.of("run", "cancel").contains(method.getName())) {
            return false;
        }

        if (method.getName().equals("run")) {
            return method.getParameterCount() == 1 &&
                    method.getReturnType() instanceof VoidType &&
                    method.getParameterType(0) instanceof ClassType &&
                    ((ClassType) method.getParameterType(0)).getFullyQualifiedName()
                            .equals(SourceFunction.SourceContext.class.getName());
        }

        if (method.getName().equals("cancel")) {
            return method.getParameterCount() == 0 && method.getReturnType() instanceof VoidType;
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (isUdfFunction(method)) {
            if (method.getName().equals("run")) {
                return TAB + "void run(SourceContext *context);" + NEW_LINE;
            }

            if (method.getName().equals("cancel")) {
                return TAB + "void cancel();" + NEW_LINE;
            }
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclClassType());

        if (methodContext.getJavaMethod().getName().equals("run") &&
                isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder();

            headBuilder.append("void ")
                    .append(className)
                    .append("::run(SourceContext *context)")
                    .append(NEW_LINE)
                    .append("{")
                    .append(NEW_LINE);

            Local paramLocal = methodContext.getParams().get(0);
            methodContext.removeLocal(paramLocal);

            headBuilder.append(TAB)
                    .append("SourceContext *").append(TranslatorUtils.formatLocalName(paramLocal))
                    .append(" = context;")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        } else if (methodContext.getJavaMethod().getName().equals("cancel") &&
                isUdfFunction(methodContext.getJavaMethod())) {

            return "void " + className + "::cancel()" + NEW_LINE + "{" + NEW_LINE;
        }

        return TranslatorUtils.printHeadAndParams(methodContext);
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(RichParallelSourceFunction.class.getName()),
                factory.getClassType(SourceFunction.SourceContext.class.getName()));
    }

    @Override
    public String printLambdaDeclare() {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }

    @Override
    public String printLambdaHeadAndParams(MethodContext methodContext) {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }
}
