/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import static com.huawei.unt.translator.TranslatorContext.NEW_LINE;
import static com.huawei.unt.translator.TranslatorContext.TAB;

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
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * TranslatorUtils
 *
 * @since 2025-05-19
 */
public class TranslatorUtils {
    private static final String ARRAY_TYPE = "Array";
    private static final String PRIMITIVE_ARRAY_TYPE = "JavaArray<%s>";
    private static final String ARRAY_HEAD = "basictypes/Arrays.h";
    private static final String JSON_HEAD = "nlohmann/json.hpp";
    private static final String CLASSCONSTANT_HEAD = "basictypes/ClassRegistry.h";
    private static final String STRINGCONSTANT_HEAD = "basictypes/StringConstant.h";

    private TranslatorUtils() {}

    /**
     * Return formatted local name
     *
     * @param local Local
     * @return formatted local name
     */
    public static String formatLocalName(Local local) {
        return local.getName().replaceAll("\\$", "").replaceAll("#", "");
    }

    /**
     * Return formatted file name
     *
     * @param fieldName fileName
     * @return formatted fieldName
     */
    public static String formatFieldName(String fieldName) {
        return fieldName.replaceAll("\\$", "_");
    }

    /**
     * Return formatted function name
     *
     * @param functionName function name
     * @return formatted function name
     */
    public static String formatFunctionName(String functionName) {
        return TranslatorContext.getFunctionMap().getOrDefault(functionName, functionName);
    }

    /**
     * Get code String from methodParams
     *
     * @param javaMethod javaMethod
     * @return params code String
     */
    public static String methodParamsToString(JavaSootMethod javaMethod) {
        StringJoiner joiner = new StringJoiner(", ");

        List<Type> params = javaMethod.getParameterTypes();
        int i = 0;
        for (Type type : params) {
            joiner.add(formatParamType(type) + "param" + i++);
        }

        return joiner.toString();
    }

    /**
     * Get code String from methodParams
     *
     * @param methodContext methodContext
     * @return code String
     */
    public static String methodParamsToString(MethodContext methodContext) {
        StringJoiner joiner = new StringJoiner(", ");
        Map<Integer, Local> params = methodContext.getParams();

        for (int i = 0; i < params.size(); i++) {
            Local local = params.get(i);
            methodContext.removeLocal(local);
            joiner.add(formatParamType(methodContext.getJavaMethod().getParameterType(i)) + formatLocalName(local));
        }

        return joiner.toString();
    }

    /**
     * Get params to String
     *
     * @param signature methodSignature
     * @param args methodArgs
     * @param methodContext methodContext
     * @return params code String
     */
    public static String paramsToString(MethodSignature signature, List<Immediate> args, MethodContext methodContext) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        StringJoiner joiner = new StringJoiner(", ");

        for (int i = 0; i < args.size(); i++) {
            Immediate immediate = args.get(i);

            immediate.accept(valueVisitor);
            String value = valueVisitor.toCode();

            if (signature.getParameterType(i) instanceof PrimitiveType.CharType
                    && immediate instanceof IntConstant) {
                value = "(char) " + value;
            }

            if (immediate instanceof StringConstant && shouldPackingString(signature)) {
                value = "StringConstant::getInstance().get(" + value + ")";
            }

            joiner.add(value);
            valueVisitor.clear();
        }

        return "(" + joiner + ")";
    }

    private static boolean shouldPackingString(MethodSignature signature) {
        Set<String> stdStringMethods = TranslatorContext.getStdStringMethods();
        String signatureStr = signature.toString();
        String declClassName = signature.getDeclClassType().getFullyQualifiedName();
        Set<String> searched = new HashSet<>();
        LinkedList<String> classQueue = new LinkedList<>();
        classQueue.add(declClassName);
        while(!classQueue.isEmpty()) {
            String className = classQueue.removeFirst();
            if(!searched.contains(className)) {
                if (stdStringMethods.contains(signatureStr.replace(declClassName, className))) {
                    stdStringMethods.add(declClassName);
                    return false;
                }
                searched.add(className);
                classQueue.addAll(TranslatorContext.getSuperclassMap().getOrDefault(className, new HashSet<>()));
            }
        }
        return true;
    }

    private static class LocalComparator implements Comparator<Local> {
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

    /**
     * print method locals
     *
     * @param methodContext methodContext
     * @return locals code string
     */
    public static String printLocals(MethodContext methodContext) {
        StringBuilder localsBuilder = new StringBuilder();
        Set<Local> locals = methodContext.getLocals();

        boolean isStaticInit = methodContext.getJavaMethod().getName()
                .equals(TranslatorContext.STATIC_INIT_FUNCTION_NAME);

        List<Local> sortedLocals = locals.stream()
                .sorted(new LocalComparator())
                .collect(Collectors.toList());

        for (Local local : sortedLocals) {
            if (local.getType() instanceof ClassType
                    && TranslatorContext.getIgnoredClasses().contains(
                            ((ClassType) local.getType()).getFullyQualifiedName())) {
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
        return localDeclare.toString();
    }

    /**
     * try to Get UdfType from classType and engineType
     *
     * @param classType classType
     * @param engineType enginType
     * @return Optional udfType
     */
    public static Optional<UDFType> getUdfType(ClassType classType, EngineType engineType) {
        for (UDFType type : EngineType.getFunctions(engineType)) {
            if (type.getBaseClass().getName().equals(classType.getFullyQualifiedName())) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    /**
     * Get formatted class name string
     *
     * @param className className
     * @return formatted className string
     */
    public static String formatClassName(String className) {
        if (TranslatorContext.getStringMap().containsKey(className)) {
            return TranslatorContext.getStringMap().get(className);
        }

        return className.replace('.', '_').replace('$', '_');
    }

    /**
     * Get micro name from className
     *
     * @param className className
     * @return mico name
     */
    public static String formatMicroName(String className) {
        return formatClassName(className).toUpperCase(Locale.ENGLISH) + "_H";
    }

    /**
     * print java class Includes
     *
     * @param javaClass javaClass
     * @return includes
     */
    public static String printIncludes(JavaClass javaClass) {
        Set<ClassType> includeClasses = javaClass.getIncludes();

        String thisClassName = formatClassName(javaClass.getClassName()) + ".h";

        Set<String> knownIncludes = new HashSet<>();
        Set<String> translatedIncludes = new HashSet<>();

        if (javaClass.isHasArray()) {
            knownIncludes.add(ARRAY_HEAD);
        }

        knownIncludes.add(CLASSCONSTANT_HEAD);
        knownIncludes.add(STRINGCONSTANT_HEAD);

        if (javaClass.isJsonConstructor()) {
            knownIncludes.add(JSON_HEAD);
        }

        for (ClassType includeClass : includeClasses) {
            String className = includeClass.getFullyQualifiedName();
            if (TranslatorContext.getIgnoredClasses().contains(className)
                    || (TranslatorContext.getStringMap().containsKey(className)
                    && !TranslatorContext.getIncludeMap().containsKey(className))) {
                continue;
            }

            Optional<String> includeString = formatIncludeString(className);

            if (includeString.isPresent()
                    && !thisClassName.equals(includeString.get())) {
                if (TranslatorContext.getStringMap().containsKey(className)) {
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

        includeBuilder.append(NEW_LINE);

        translatedIncludes.stream().sorted().forEach(include -> includeBuilder
                .append("#include \"")
                .append(include)
                .append("\"")
                .append(NEW_LINE));

        Set<PrimitiveType> primitiveTypeSet = new HashSet<>();
        for (JavaSootField field : javaClass.getFields()) {
            if (field.getType() instanceof PrimitiveType) {
                if (!TranslatorContext.PRIMITIVE_TYPE_INCLUDESTRING_MAP.containsKey(field.getType())) {
                    throw new TranslatorException("no support " + ((PrimitiveType) field.getType()).getName() + "primitive type");
                }else {
                    primitiveTypeSet.add((PrimitiveType) field.getType());
                }
            }
        }
        for (PrimitiveType primitiveType : primitiveTypeSet) {
            includeBuilder.append("#include \"")
                    .append(TranslatorContext.PRIMITIVE_TYPE_INCLUDESTRING_MAP.get(primitiveType))
                    .append("\"")
                    .append(NEW_LINE);
        }

        return includeBuilder.toString();
    }

    private static Optional<String> formatIncludeString(String includeClass) {
        if (TranslatorContext.getIncludeMap().containsKey(includeClass)) {
            return Optional.of(TranslatorContext.getIncludeMap().get(includeClass));
        }

        Type type = JavaIdentifierFactory.getInstance().getType(includeClass);

        if (type instanceof ClassType) {
            return Optional.of(formatClassName(((ClassType) type).getFullyQualifiedName()) + ".h");
        }

        return Optional.empty();
    }

    /**
     * get head and params from methodContext
     *
     * @param methodContext methodContext
     * @return head and params string
     */
    public static String printHeadAndParams(MethodContext methodContext) {
        return TranslatorUtils.formatFunctionType(methodContext.getJavaMethod().getReturnType())
                + formatType(methodContext.getJavaMethod().getDeclClassType())
                + "::"
                + methodContext.getJavaMethod().getName()
                + "("
                + methodParamsToString(methodContext)
                + ")"
                + NEW_LINE
                + "{"
                + NEW_LINE;
    }

    /**
     * Get declare method cpp code string from javaMethod
     *
     * @param javaMethod javaMethod
     * @return declare method string
     */
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

    /**
     * format function type
     *
     * @param type function type
     * @return function type string
     */
    public static String formatFunctionType(Type type) {
        if (type instanceof VoidType) {
            return "void ";
        } else {
            return formatParamType(type);
        }
    }

    /**
     * check type is primary type
     *
     * @param type type
     * @return primary or not
     */
    public static boolean isPrimaryType(Type type) {
        return type instanceof PrimitiveType
                || type instanceof VoidType;
    }

    /**
     * format param type to string
     *
     * @param type type
     * @return param type string
     */
    public static String formatParamType(Type type) {
        String typeName = formatType(type);
        return isPrimaryType(type) ? typeName + " " : typeName + " *";
    }

    /**
     * format type to string
     *
     * @param type type
     * @return type string
     */
    public static String formatType(Type type) {
        if (type instanceof ArrayType) {
            if (((ArrayType) type).getBaseType() instanceof PrimitiveType) {
                return String.format(PRIMITIVE_ARRAY_TYPE, ((ArrayType) type).getBaseType());
            }
            return ARRAY_TYPE;
        }

        return TranslatorTypeVisitor.getTypeString(type);
    }

    /**
     * Return engine type from engine name
     *
     * @param engineName enginName
     * @return enginType from name
     */
    public static EngineType getEngineType(String engineName) {
        switch (engineName.toUpperCase(Locale.ENGLISH)) {
            case "FLINK":
                return EngineType.FLINK;
            default:
                throw new TranslatorException("Unknown engine type: " + engineName);
        }
    }

    /**
     * return ClassType From classname
     *
     * @param className class name
     * @return ClassType
     */
    public static ClassType getClassTypeFromClassName(String className) {
        return JavaIdentifierFactory.getInstance().getClassType(className);
    }

    /**
     * check method is ignored method
     *
     * @param method method
     * @return it is ignored or not
     */
    public static boolean isIgnoredMethod(JavaSootMethod method) {
        String name = method.getDeclClassType().getFullyQualifiedName() + "::" + method.getName();
        return TranslatorContext.getIgnoredMethods().contains(name);
    }

    /**
     * get jar hash path
     *
     * @param jarFilePath jar file path
     * @return JarHashPath
     */
    public static String getJarHashPath(String jarFilePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(jarFilePath)) {
                byte[] byteArray = new byte[1024];
                int byteCount;
                while ((byteCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, byteCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append("0");
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException|IOException e) {
            throw new TranslatorException("Failed to get jar hash path");
        }
    }

    public static String parseSignature(String signature){
        if (signature == null || signature.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        int[] pos = {0}; //
        result.append(parseType(signature, pos));
        return result.toString();
    }
    private static String parseType(String sig, int[] pos) {
        if (pos[0] >= sig.length()) return "";

        char ch = sig.charAt(pos[0]);

        //
        if (ch == '[') {
            pos[0]++;
            return parseType(sig, pos) + "[]";
        }

        // /
        if (ch == 'L') {
            pos[0]++;
            StringBuilder className = new StringBuilder();

            while (pos[0] < sig.length()) {
                char c = sig.charAt(pos[0]);

                if (c == '<') {
                    pos[0]++; //  '<'
                    List<String> typeArgs = new ArrayList<>();
                    while (sig.charAt(pos[0]) != '>') {
                        typeArgs.add(parseType(sig, pos));
                    }
                    pos[0]++; //  '>'
                    String qualified = className.toString().replace('/', '.');
                    return qualified + "<" + String.join(",", typeArgs) + ">";
                } else if (c == ';') {
                    pos[0]++;
                    return className.toString().replace('/', '.');
                } else {
                    className.append(c);
                    pos[0]++;
                }
            }
        }

        //
        pos[0]++;
        switch (ch) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            case 'V': return "void";
            default:  return "<?>"; //
        }
    }
}
