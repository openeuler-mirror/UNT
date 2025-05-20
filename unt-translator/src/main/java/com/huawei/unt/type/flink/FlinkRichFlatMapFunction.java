package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class FlinkRichFlatMapFunction implements UDFType {
    public static final FlinkRichFlatMapFunction INSTANCE = new FlinkRichFlatMapFunction();

    @Override
    public Class<?> getBaseClass() {
        return RichFlatMapFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<RichFlatMapFunction<Object>> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
    }

    @Override
    public String getSoPrefix() {
        return "librichflatmap";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!ImmutableSet.of("flatMap", "open").contains(method.getName())) {
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

        if (method.getName().equals("open")) {
            return method.getParameterCount() == 1
                    && method.getReturnType() instanceof VoidType
                    && method.getParameterType(0) instanceof ClassType
                    && ((ClassType) method.getParameterType(0)).getClassName().equals("Configuration");
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (isUdfFunction(method) &&
                method.getName().equals("flatMap")) {
            return "    void flatMap(Object *obj, Collector *collector) override;" + NEW_LINE;
        } else if (isUdfFunction(method) &&
                method.getName().equals("open")) {
            return "    void open(const Configuration& conf) override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }


    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclClassType());

        if (methodContext.getJavaMethod().getName().equals("flatMap") &&
                isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder("void ")
                    .append(className)
                    .append("::flatMap(Object *obj, Collector *collector)")
                    .append(NEW_LINE)
                    .append("{")
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
        } else if (methodContext.getJavaMethod().getName().equals("open") &&
                isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder()
                    .append("void ")
                    .append(className)
                    .append("::open(const Configuration& conf)")
                    .append(NEW_LINE)
                    .append("{")
                    .append(NEW_LINE);

            Local paramLocal = methodContext.getParams().get(0);
            methodContext.removeLocal(paramLocal);

            headBuilder.append(TAB)
                    .append("Configuration *")
                    .append(TranslatorUtils.formatLocalName(paramLocal))
                    .append(" = const_cast<Configuration *>(&conf);")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(RichFlatMapFunction.class.getName()),
                factory.getClassType(Collector.class.getName()),
                factory.getClassType(Configuration.class.getName()));
    }
}
