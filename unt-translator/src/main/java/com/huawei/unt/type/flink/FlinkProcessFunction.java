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
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import org.apache.flink.streaming.api.functions.ProcessFunction;

import java.util.Set;

public class FlinkProcessFunction implements UDFType {
    public static final FlinkProcessFunction INSTANCE = new FlinkProcessFunction();

    @Override
    public Class<?> getBaseClass() {
        return ProcessFunction.class;
    }

    @Override
    public String getCppFileString(String className) {
        return "#include \"../" + className + ".h\"\n\n"
                + "extern \"C\" std::unique_ptr<ProcessFunction<Object*, Object*>> NewInstance(nlohmann::json jsonObj) {\n"
                + "    return std::make_unique<" + className + ">(jsonObj);\n"
                + "}";
    }

    @Override
    public Set<ClassType> getRequiredIncludes() {
        JavaIdentifierFactory factory = JavaIdentifierFactory.getInstance();
        return ImmutableSet.of(factory.getClassType(ProcessFunction.class.getName()));
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

        if (!ImmutableSet.of("processElement", "open").contains(method.getName())) {
            return false;
        }

        if (method.getModifiers().contains(MethodModifier.BRIDGE)) {
            return false;
        }

        if (method.getName().equals("processElement")) {
            return method.getParameterCount() == 3
                    && method.getReturnType() instanceof VoidType
                    && method.getParameterType(0) instanceof ClassType
                    && method.getParameterType(1) instanceof ClassType
                    && ((ClassType) method.getParameterType(1)).getClassName().equals("ProcessFunction$Context")
                    && method.getParameterType(2) instanceof ClassType
                    && ((ClassType) method.getParameterType(2)).getClassName().equals("Collector");
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
        if (isUdfFunction(method) && method.getName().equals("processElement")) {
            return "    void processElement("
                    + "Object *obj, ProcessFunction<Object*, Object*>::Context *ctx, Collector *collector"
                    + ") override;" + NEW_LINE;
        }

        if (isUdfFunction(method) && "open".equals(method.getName())) {
            return "    void open(const Configuration& conf) override;" + NEW_LINE;
        }

        return TranslatorUtils.printDeclareMethod(method);
    }

    @Override
    public String printHeadAndParams(MethodContext methodContext) {
        String className = TranslatorUtils.formatClassName(
                methodContext.getJavaMethod().getDeclClassType().getFullyQualifiedName());
        if (methodContext.getJavaMethod().getName().equals("processElement")
                && isUdfFunction(methodContext.getJavaMethod())) {

            StringBuilder headBuilder = new StringBuilder("void ")
                    .append(className)
                    .append("::processElement(")
                    .append("Object *obj, ProcessFunction<Object*, Object*>::Context *ctx, Collector *collector) {")
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
                    .append("ProcessFunction<Object*, Object*>::Context *")
                    .append(TranslatorUtils.formatLocalName(param2))
                    .append(" = ctx;")
                    .append(NEW_LINE);

            Local param3 = methodContext.getParams().get(2);
            methodContext.removeLocal(param3);
            headBuilder.append(TAB)
                    .append("Collector *")
                    .append(TranslatorUtils.formatLocalName(param3))
                    .append(" = collector;")
                    .append(NEW_LINE);

            return headBuilder.append(NEW_LINE).toString();
        }  else if ("open".equals(methodContext.getJavaMethod().getName())
                && isUdfFunction(methodContext.getJavaMethod())) {
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
}
