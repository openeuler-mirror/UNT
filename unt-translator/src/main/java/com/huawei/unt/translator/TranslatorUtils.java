package com.huawei.unt.translator;

import com.huawei.unt.model.JavaClass;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.visitor.TranslatorTypeVisitor;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;
import com.huawei.unt.type.EngineType;
import com.huawei.unt.type.UDFType;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.VoidType;
import sootup.core.types.Type;
import sootup.core.types.ArrayType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

public class TranslatorUtils {

    public static String formatLocalName(Local local) {
        return local.getName().replaceAll("\\$", "").replaceAll("#", "");
    }

    public static String formatFieldName(String fieldName) {
        return fieldName.replaceAll("\\$", "_");
    }

    public static String formatFunctionName(String functionName) {
        return TranslatorContext.FUNCTION_MAP.getOrDefault(functionName, functionName);
    }

    public static String methodParamsToString(JavaSootMethod javaMethod) {
        StringJoiner joiner = new StringJoiner(", ");

        for (int i = 0; i < javaMethod.getParameterCount(); i++) {
            joiner.add(formatParamType(javaMethod.getParameterType(i)) + "param" + i);
        }

        return joiner.toString();
    }

    public static String methodParamsToString(MethodContext methodContext) {
        StringJoiner joiner = new StringJoiner(", ");
        Map<Integer, Local> params = methodContext.getParams();

        for (int i = 0; i < params.size(); i++) {
            Local local = params.get(i);
            methodContext.removeLocal(local);
            joiner.add(formatParamType(local.getType()) + formatLocalName(local));
        }

        return joiner.toString();
    }

    public static String paramsToString(MethodSignature signature, List<Immediate> args, MethodContext methodContext) {

        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        StringJoiner joiner = new StringJoiner(", ");

        // todo: deal with string constant packing, check if need remove it
        boolean needPackingString = !TranslatorContext.STD_STRING_METHODS
                .contains(signature.toString());


        for (int i = 0; i < args.size(); i++) {
            Immediate immediate = args.get(i);

            immediate.accept(valueVisitor);
            String value = valueVisitor.toCode();

            if (signature.getParameterType(i) instanceof PrimitiveType.CharType &&
                    immediate instanceof IntConstant) {
                value = "(char) " + value;
            }

            if (immediate instanceof StringConstant && needPackingString) {
                value = "StringConstant::getInstance().get(" + value + ")";
            }

            joiner.add(value);
            valueVisitor.clear();
        }

        return "(" + joiner + ")";
    }

    private static class  localComparator implements Comparator<Local> {
        @Override
        public int compare(Local o1, Local o2) {
            String type1 = o1.getType().toString();
            String type2 = o2.getType().toString();
            if (!type1.equals(type2)) {
                return type1.compareTo(type2);
            } else {
                String name1 = o1.getName();
                String name2 = o2.getName();
                return name1.compareTo(name2);
            }
        }
    }



    public static String printLocals(MethodContext methodContext) {
        StringBuilder localsBuilder = new StringBuilder();
        Set<Local> locals = methodContext.getLocals();

        boolean isStaticInit = methodContext.getJavaMethod().getName()
                .equals(TranslatorContext.STATIC_INIT_FUNCTION_NAME);

        List<Local> sortedLocals = locals.stream()
                .sorted(new localComparator())
                .collect(Collectors.toList());

        for (Local local : sortedLocals) {
//            if (methodContext.isMergedLocal(local)) {
//                continue;
//            }

            if (local.getType() instanceof ClassType &&
                    TranslatorContext.IGNORED_CLASSES.contains(((ClassType) local.getType()).getFullyQualifiedName())) {
                continue;
            }

            if (!isStaticInit) {
                localsBuilder.append(TAB);
            } else {
                localsBuilder.append(TAB).append(TAB);
            }

            localsBuilder.append(printLocalWithType(local))
                    .append(";")
                    .append(NEW_LINE);
        }

        if (localsBuilder.length() > 0) {
            return localsBuilder + NEW_LINE;
        }

        return localsBuilder.toString();
    }

    private static String printLocalWithType(Local local) {
        StringBuilder localDeclare = new StringBuilder();
        localDeclare.append(formatParamType(local.getType())).append(formatLocalName(local));
        if (! (local.getType() instanceof PrimitiveType)) {
            localDeclare.append(" = nullptr");
        }
        return  localDeclare.toString();
    }

    public static Optional<UDFType> getUdfType(ClassType classType, EngineType engineType) {
        for (UDFType type : EngineType.getFunctions(engineType)) {
            if (type.getBaseClass().getName().equals(classType.getFullyQualifiedName())) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    public static String formatClassName(String className) {
        if (TranslatorContext.CLASS_MAP.containsKey(className)) {
            return TranslatorContext.CLASS_MAP.get(className);
        }

        return className.replace('.', '_').replace('$', '_');
    }

    public static String formatMicroName(String className) {

        return formatClassName(className).toUpperCase(Locale.ENGLISH) + "_H";
    }

    public static String printIncludes(JavaClass javaClass) {
        Set<ClassType> includeClasses = javaClass.getIncludes();

        String thisClassName = formatClassName(javaClass.getClassName()) + ".h";

//        Set<String> includes = new HashSet<>();

        Set<String> knownIncludes = new HashSet<>();
        Set<String> translatedIncludes = new HashSet<>();

        if (javaClass.isHasArray()) {
            knownIncludes.add(ARRAY_HEAD);
        }

        knownIncludes.add(CLASSCONSTANT_HEAD);
        knownIncludes.add(STRINGCONSTANT_HEAD);

        if (javaClass.isJsonConstructor()){
            knownIncludes.add(JSON_HEAD);
        }

        for (ClassType includeClass : includeClasses) {
            String className = includeClass.getFullyQualifiedName();
            if (TranslatorContext.IGNORED_CLASSES.contains(className) ||
                    (TranslatorContext.CLASS_MAP.containsKey(className) &&
                            !TranslatorContext.INCLUDE_MAP.containsKey(className)) ) {
                continue;
            }

            Optional<String> includeString = formatIncludeString(className);

            if (includeString.isPresent() &&
                    !thisClassName.equals(includeString.get())) {
                if (TranslatorContext.CLASS_MAP.containsKey(className)) {
                    knownIncludes.add(includeString.get());
                } else {
                    translatedIncludes.add(includeString.get());
                }
            }
        }

        StringBuilder includeBuilder = new StringBuilder();

        knownIncludes.stream().sorted().forEach(include -> includeBuilder
                .append("#include \"")
                .append(include)
                .append("\"")
                .append(NEW_LINE));

//        includeBuilder.append("#include \"basictypes/functions/types/ClassConstant.h\"").append(NEW_LINE);

        includeBuilder.append(NEW_LINE);

        translatedIncludes.stream().sorted().forEach(include -> includeBuilder
                .append("#include \"")
                .append(include)
                .append("\"")
                .append(NEW_LINE));

        return includeBuilder.toString();
    }

    private static Optional<String> formatIncludeString(String includeClass) {
        if (TranslatorContext.INCLUDE_MAP.containsKey(includeClass)) {
            return Optional.of(TranslatorContext.INCLUDE_MAP.get(includeClass));
        }

        Type type = JavaIdentifierFactory.getInstance().getType(includeClass);

        if (type instanceof ClassType) {
            return Optional.of(formatClassName(((ClassType) type).getFullyQualifiedName()) + ".h");
        }

        return Optional.empty();
    }

    public static String printHeadAndParams(MethodContext methodContext) {
        return TranslatorUtils.formatFunctionType(methodContext.getJavaMethod().getReturnType()) +
                formatType(methodContext.getJavaMethod().getDeclClassType()) +
                "::" +
                methodContext.getJavaMethod().getName() +
                "(" +
                methodParamsToString(methodContext) +
                ")" +
                NEW_LINE +
                "{" +
                NEW_LINE;
    }

    public static String printDeclareMethod(JavaSootMethod javaMethod) {
        if (javaMethod.getName().equals(TranslatorContext.STATIC_INIT_FUNCTION_NAME)) {
            return "";
        }

        if (javaMethod.getName().equals(TranslatorContext.INIT_FUNCTION_NAME)) {
            return printDeclareInitMethod(javaMethod);
        }

        StringBuilder headBuilder = new StringBuilder(TAB);

        if (javaMethod.isStatic()) {
            headBuilder.append("static ");
        }

        if (javaMethod.isAbstract()) {
            headBuilder.append("virtual ");
        }

        headBuilder.append(TranslatorUtils.formatFunctionType(javaMethod.getReturnType()))
                .append(javaMethod.getName())
                .append("(")
                .append(methodParamsToString(javaMethod))
                .append(")");

        if (javaMethod.isAbstract()) {
            headBuilder.append(" = 0");
        }

        return headBuilder.append(";").append(NEW_LINE).toString();
    }

    private static String printDeclareInitMethod(JavaSootMethod javaMethod) {
        StringBuilder headBuilder = new StringBuilder(TAB);

        if (javaMethod.getParameterCount() == 1) {
            headBuilder.append("explicit ");
        }

        String className = formatType(javaMethod.getDeclClassType());

        return headBuilder.append(className)
                .append("(")
                .append(methodParamsToString(javaMethod))
                .append(");").append(NEW_LINE).toString();
    }

    private static final String ARRAY_TYPE = "Array";
    private static final String ARRAY_HEAD = "basictypes/Arrays.h";
    private static final String JSON_HEAD = "nlohmann/json.hpp";
    private static final String CLASSCONSTANT_HEAD = "basictypes/ClassConstant.h";
    private static final String STRINGCONSTANT_HEAD = "basictypes/StringConstant.h";

    public static String formatFunctionType(Type type) {
        if (type instanceof VoidType) {
            return "void ";
        } else {
            return formatParamType(type);
        }
    }

    public static boolean isPrimaryType(Type type) {
        String typeString = formatType(type);

        return type instanceof PrimitiveType ||
                type instanceof VoidType ||
                TranslatorContext.PRIMARY_TYPES.contains(typeString);
    }

    public static String formatParamType(Type type) {
        String typeName = formatType(type);
        return isPrimaryType(type) ? typeName + " " : typeName + " *";
    }

    public static String formatType(Type type) {
        if (type instanceof ArrayType) {
            return ARRAY_TYPE;
        }

        return TranslatorTypeVisitor.getTypeString(type);
    }

    public static EngineType getEngineType(String engineName) {
        switch (engineName.toUpperCase(Locale.ENGLISH)) {
            case "FLINK":
                return EngineType.FLINK;
            default:
                throw new TranslatorException("Unknown engine type: " + engineName);
        }
    }

    public static ClassType getClassTypeFromClassName(String className) {
        return JavaIdentifierFactory.getInstance().getClassType(className);
    }

    public static boolean isIgnoredMethod(JavaSootMethod method) {
        String name = method.getDeclClassType().getFullyQualifiedName() + "::" + method.getName();
        return TranslatorContext.IGNORED_METHODS.contains(name);
    }

    public static String getJarHashPath(String jarFilePath){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(jarFilePath)){
                byte[] byteArray = new byte[1024];
                int byteCount;
                while ((byteCount = fis.read(byteArray)) != -1){
                    digest.update(byteArray,0,byteCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1){
                    sb.append("0");
                }
                sb.append(hex);
            }
            String jarPath = sb.toString();
            return jarPath;
        } catch (Exception e) {
            throw new TranslatorException("Failed to get jar hash path");
        }
    }

    private TranslatorUtils() {}
}
