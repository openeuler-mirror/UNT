package com.huawei.unt.type.flink;

import com.google.common.collect.ImmutableSet;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.UDFType;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.configuration.Configuration;
import sootup.core.jimple.basic.Local;
import sootup.core.model.MethodModifier;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.util.Set;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class FlinkRichFilterFunction implements UDFType {
    public static final FlinkRichFilterFunction INSTANCE = new FlinkRichFilterFunction();

    @Override
    public Class<?> getBaseClass() {
        return RichFilterFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"" + NEW_LINE + NEW_LINE
                + "extern \"C\" std::unique_ptr<FilterFunction> NewInstance(nlohmann::json jsonObj) {" + NEW_LINE
                + "    return std::make_unique<" + className + ">(jsonObj);" + NEW_LINE
                + "}";
    }

    @Override
    public String getSoPrefix() {
        return "librichfilter";
    }

    @Override
    public boolean isUdfFunction(JavaSootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        if (!ImmutableSet.of("filter", "open").contains(method.getName())) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        if (method.getName().equals("filter")) {
            return method.getParameterCount() == 1
                    && method.getReturnType() instanceof PrimitiveType.BooleanType
                    && method.getParameterType(0) instanceof ClassType;
        }

        if (method.getName().equals("open")) {
            return method.getParameterCount() == 1
                    && method.getParameterType(0) instanceof ClassType
                    && ((ClassType) method.getParameterType(0)).getClassName().equals("Configuration");
        }

        return false;
    }

    @Override
    public String printDeclareMethod(JavaSootMethod method) {
        if (isUdfFunction(method) && method.getName().equals("filter")) {
            return "    bool filter(Object *obj) override;" + NEW_LINE;
        }
//        else if (isUdfFunction(method) && method.getName().equals("open")) {
//            return "    void open(const Configuration& conf) override;" + NEW_LINE;
//        }

        return TranslatorUtils.printDeclareMethod(method);
    }


    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatType(methodContext.getJavaMethod().getDeclClassType());

        if (methodContext.getJavaMethod().getName().equals("filter")
                && isUdfFunction(methodContext.getJavaMethod())) {
            StringBuilder headBuilder = new StringBuilder();

            headBuilder.append("bool ")
                    .append(className)
                    .append("::filter(Object *obj)")
                    .append(NEW_LINE)
                    .append("{")
                    .append(NEW_LINE);

            Local paramLocal = methodContext.getParams().get(0);
            methodContext.removeLocal(paramLocal);

            String typeString = TranslatorUtils.formatType(paramLocal.getType());

            headBuilder.append(TAB)
                    .append(typeString).append(" *").append(TranslatorUtils.formatLocalName(paramLocal))
                    .append(" = reinterpret_cast<").append(typeString).append(" *>(obj);")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
//        } else if (methodContext.getJavaMethod().getName().equals("open")
//                && isUdfFunction(methodContext.getJavaMethod())) {
//            StringBuilder headBuilder = new StringBuilder()
//                    .append("void ")
//                    .append(className)
//                    .append("::open(const Configuration& conf)")
//                    .append(NEW_LINE)
//                    .append("{")
//                    .append(NEW_LINE);
//
//            Local paramLocal = methodContext.getParams().get(0);
//            methodContext.removeLocal(paramLocal);
//
//            headBuilder.append(TAB)
//                    .append("Configuration *")
//                    .append(TranslatorUtils.formatLocalName(paramLocal))
//                    .append(" = const_cast<Configuration *>(&conf);")
//                    .append(NEW_LINE);
//
//            return headBuilder.append(NEW_LINE).toString();
        } else {
            return TranslatorUtils.printHeadAndParams(methodContext);
        }
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(RichFilterFunction.class.getName()),
                factory.getClassType(Configuration.class.getName()));
    }
}
