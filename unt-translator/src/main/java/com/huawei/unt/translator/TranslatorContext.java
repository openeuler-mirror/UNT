/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import sootup.core.types.PrimitiveType;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * TranslatorContext
 *
 * @since 2025-05-19
 */
public class TranslatorContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorContext.class);

    /**
     * line separator
     */
    public static final String NEW_LINE = System.lineSeparator();

    /**
     * java init function name
     */
    public static final String INIT_FUNCTION_NAME = "<init>";

    /**
     * java static init function name
     */
    public static final String STATIC_INIT_FUNCTION_NAME = "<clinit>";

    /**
     * four space for tab
     */
    public static final String TAB = "    ";

    /**
     * new object expression
     */
    public static final String NEW_OBJ = "new %s(%s)";

    /**
     * unknown put ref cpp code string
     */
    public static final String UNKNOWN_PUT_REF = TAB + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB + TAB + "%1$s->putRefCount();" + NEW_LINE
            + TAB + "}" + NEW_LINE;

    /**
     * circle put ref cpp code string
     */
    public static final String CIRCLE_PUT_REF = TAB + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB + TAB + "%1$s->putRefCount();" + NEW_LINE
            + TAB + TAB + "%1$s = nullptr;" + NEW_LINE
            + TAB + "}" + NEW_LINE;

    /**
     * get ref cpp code string
     */
    public static final String GET_REF = TAB + "if (%1$s != nullptr) {" + NEW_LINE
            + TAB + TAB + "%1$s->getRefCount();" + NEW_LINE + TAB + "}" + NEW_LINE;

    /**
     * Make null code string
     */
    public static final String MAKE_NULL = TAB + "%s = nullptr;" + NEW_LINE;

    /**
     * ref assign code string
     */
    public static final String RET_ASSIGN = TAB + "ret = %s;" + NEW_LINE;

    /**
     * goto free code string
     */
    public static final String GOTO_FREE = TAB + "goto free;" + NEW_LINE;

    /**
     * return ret code string
     */
    public static final String RETURN_RET = TAB + "return ret;" + NEW_LINE;

    /**
     * tmp object declare code string
     */
    public static final String TMP_OBJ_DECLARE = TAB + "Object *tmpObj = nullptr;" + NEW_LINE;

    /**
     * tmp object assign code string
     */
    public static final String TMP_OBJ_ASSIGN = TAB + "Object *tmpObj = %s;" + NEW_LINE;

    /**
     * tmp object free object
     */
    public static final String TMP_OBJ_FREE = String.format(CIRCLE_PUT_REF, "tmpObj");

    /**
     * old var declare code string
     */
    public static final String OLD_VAR_DECLARE = TAB + "%1$sold%2$S;" + NEW_LINE;

    /**
     * old var assign code string
     */
    public static final String OLD_VAR_ASSIGN = TAB + "old%1$S = %1$s;" + NEW_LINE;

    /**
     * old var put code string
     */
    public static final String OLD_VAR_PUT = TAB + "if (old%1$S != nullptr) {" + NEW_LINE
            + TAB + TAB + "old%1$S->putRefCount();" + NEW_LINE
            + TAB + "}" + NEW_LINE;

    /**
     *
     */
    public static final Map<PrimitiveType, String> PRIMITIVE_TYPE_STRING_MAP =
            new HashMap<PrimitiveType, String>(){{
                put(PrimitiveType.BooleanType.getInstance(), "Boolean");
                put(PrimitiveType.IntType.getInstance(), "Integer");
                put(PrimitiveType.DoubleType.getInstance(), "Double");
                put(PrimitiveType.LongType.getInstance(), "Long");
            }};

    /**
     *
     */
    public static final Map<PrimitiveType, String> PRIMITIVE_TYPE_INCLUDESTRING_MAP =
            new HashMap<PrimitiveType, String>(){{
                put(PrimitiveType.BooleanType.getInstance(), "basictypes/java_lang_Boolean.h");
                put(PrimitiveType.IntType.getInstance(), "basictypes/Integer.h");
                put(PrimitiveType.DoubleType.getInstance(), "basictypes/Double.h");
                put(PrimitiveType.LongType.getInstance(), "basictypes/Long.h");
            }};

    private static Map<String, Set<String>> superclassMap = new HashMap<>();
    private static Map<String, Set<String>> subclassMap = new HashMap<>();
    private static Map<String, Set<String>> missingInterfaces = new HashMap<>();
    private static Map<String, String> udfMap = new HashMap<>();
    private static Map<String, String> stringMap = new HashMap<>();
    private static Map<String, String> includeMap = new HashMap<>();
    private static Map<String, String> functionMap = new HashMap<>();
    private static Map<String, Integer> libInterfaceRef = new HashMap<>();
    private static Set<String> filterPackages = new HashSet<>();
    private static Set<String> ignoredClasses = new HashSet<>();
    private static Set<String> ignoredMethods = new HashSet<>();
    private static Set<String> stdStringMethods = new HashSet<>();
    private static Map<String, String> genericFunction = new HashMap<>();

    private static int tuneLevel;
    private static boolean isMemTune;
    private static boolean isHwAccTune;
    private static boolean isRegexAcc;
    private static String compileOption;

    private TranslatorContext() {
    }

    /**
     * init translator config
     *
     * @param configDir base config dir
     */
    public static void init(String configDir) {
        LOGGER.info("Init TranslatorContext");

        String udfConfigDir = (configDir.endsWith(File.separator) ? configDir : configDir + File.separator)
                + "udf_tune.properties";
        LOGGER.info("load properties: {}", udfConfigDir);
        Properties udfProperties = new Properties();

        try {
            udfProperties.load(Files.newInputStream(Paths.get(udfConfigDir)));
            LOGGER.info("load udf config");
            for (Object key : udfProperties.keySet()) {
                LOGGER.info("load [{}] = [{}]", key, udfProperties.getProperty(key.toString()));
                getUdfMap().put(key.toString(), udfProperties.getProperty(key.toString()));
            }
        } catch (IOException e) {
            throw new TranslatorException("Load udf_config files failed: " + e.getMessage());
        }

        if (!getUdfMap().containsKey("basic_lib_path")) {
            throw new TranslatorException("basic_lib_path is not set");
        }
        if (!getUdfMap().containsKey("tune_level")) {
            LOGGER.info("tune_level is not configured, use default tune_level 0");
            tuneLevel = 0;
        } else {
            int tLevel = Integer.parseInt(getUdfMap().get("tune_level"));
            if (tLevel > 4 || tLevel < 0) {
                LOGGER.info("tune_level is incorrectly configured, use default tune_level 0");
                tuneLevel = 0;
            } else {
                LOGGER.info("using tune_level {}", tLevel);
                tuneLevel = tLevel;
            }
        }
        if ((getTuneLevel() & 2) != 0) {
            isMemTune = true;
            LOGGER.info("Enabling Memory Optimization");
        } else {
            isMemTune = false;
            LOGGER.info("Use the default memory policy.");
        }

        if ((getTuneLevel() & 1) != 0) {
            isHwAccTune = true;
            LOGGER.info("Enabling Hardware Acceleration Optimization");
        } else {
            isHwAccTune = false;
            LOGGER.info("Disabling Hardware Acceleration Optimization.");
        }

        if (isIsHwAccTune() && "1".equals(getUdfMap().getOrDefault("regex_lib_type", "0"))) {
            isRegexAcc = true;
            LOGGER.info("Enabling Regex acc.");
        } else {
            isRegexAcc = false;
            LOGGER.info("Disabling Regex acc.");
        }

        if (!getUdfMap().containsKey("compile_option") || "".equals(getUdfMap().get("compile_option"))) {
            compileOption = "-O3 -std=c++17 -fPIC";
            LOGGER.info("use default compile_option: -O3 -std=c++17 -fPIC");
        } else {
            compileOption = getUdfMap().get("compile_option");
            LOGGER.info("use compile_option: {}", getCompileOption());
        }

        String basicDir = getUdfMap().get("basic_lib_path").endsWith(File.separator)
                ? getUdfMap().get("basic_lib_path")
                : getUdfMap().get("basic_lib_path") + File.separator;

        LOGGER.info("load conf base");

        String dependClassProfile = basicDir + "conf" + File.separator + "depend_class.properties";
        String functionProfile = basicDir + "conf" + File.separator + "function.properties";
        String dependIncludeProfile = basicDir + "conf" + File.separator + "depend_include.properties";
        String ignorePackageProfile = basicDir + "conf" + File.separator + "ignoredPackage.config";
        String ignoreClassProfile = basicDir + "conf" + File.separator + "ignoredClasses.config";
        String ignoredMethodsProfile = basicDir + "conf" + File.separator + "ignoredMethods.config";
        String stdStringMethodsProfile = basicDir + "conf" + File.separator + "stdStringMethods.config";
        String dependInterfaceInfo = basicDir + "conf" + File.separator + "depend_interface.config";
        String genericFunctionInfo = basicDir + "conf" + File.separator + "udf_generic.config";

        Properties classProperties = new Properties();
        Properties functionProperties = new Properties();
        Properties includeProperties = new Properties();
        Set<String> filterPackageSet = new HashSet<>();
        Set<String> ignoreClassesSet = new HashSet<>();
        Set<String> ignoredMethodsSet = new HashSet<>();
        Set<String> stdStringMethodSet = new HashSet<>();
        Map<String, Integer> dependInterfaces = new HashMap<>();
        Map<String, String> genericFunctionsMap = new HashMap<>();

        try (BufferedReader ignoredPackageReader = Files.newBufferedReader(Paths.get(ignorePackageProfile));
             BufferedReader ignoredClassReader = Files.newBufferedReader(Paths.get(ignoreClassProfile));
             BufferedReader ignoredMethodReader = Files.newBufferedReader(Paths.get(ignoredMethodsProfile));
             BufferedReader stdStringMethodReader = Files.newBufferedReader(Paths.get(stdStringMethodsProfile));
             BufferedReader dependInterfacesReader = Files.newBufferedReader(Paths.get(dependInterfaceInfo));
             BufferedReader genericFunctionReader = Files.newBufferedReader(Paths.get(genericFunctionInfo));
             InputStream dependClassStream = Files.newInputStream(Paths.get(dependClassProfile));
             InputStream functionStream = Files.newInputStream(Paths.get(functionProfile));
             InputStream includeStream = Files.newInputStream(Paths.get(dependIncludeProfile))) {
            classProperties.load(dependClassStream);
            functionProperties.load(functionStream);
            includeProperties.load(includeStream);

            String ignorePackage;
            while ((ignorePackage = ignoredPackageReader.readLine()) != null) {
                filterPackageSet.add(ignorePackage.trim());
            }

            String ignoredClass;
            while ((ignoredClass = ignoredClassReader.readLine()) != null) {
                ignoreClassesSet.add(ignoredClass.trim());
            }

            String ignoredMethod;
            while ((ignoredMethod = ignoredMethodReader.readLine()) != null) {
                ignoredMethodsSet.add(ignoredMethod.trim());
            }

            String stdStringMethod;
            while ((stdStringMethod = stdStringMethodReader.readLine()) != null) {
                stdStringMethodSet.add(stdStringMethod);
            }

            String dependInterface;
            while ((dependInterface = dependInterfacesReader.readLine()) != null) {
                if (dependInterface.startsWith("%")) {
                    continue;
                }
                String[] ref = dependInterface.trim().split(", ");
                dependInterfaces.put(ref[0].trim(), Integer.valueOf(ref[1].trim()));
            }

            String grcFunction;
            while ((grcFunction = genericFunctionReader.readLine()) != null) {
                if (grcFunction.startsWith("%")) {
                    continue;
                }
                String[] ref = grcFunction.trim().split(": ");
                genericFunctionsMap.put(ref[0].trim(), ref[1].trim());
            }
        } catch (IOException e) {
            throw new TranslatorException("Load config files failed: " + e.getMessage());
        }

        LOGGER.info("load class config:");
        for (Object key : classProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, classProperties.getProperty(key.toString()));
            stringMap.put(key.toString(), classProperties.getProperty(key.toString()));
        }

        LOGGER.info("load function config:");
        for (Object key : functionProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, functionProperties.getProperty(key.toString()));
            functionMap.put(key.toString(), functionProperties.getProperty(key.toString()));
        }

        LOGGER.info("load include config:");
        for (Object key : includeProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, includeProperties.getProperty(key.toString()));
            includeMap.put(key.toString(), includeProperties.getProperty(key.toString()));
        }

        LOGGER.info("load package filter config:");
        filterPackages.addAll(filterPackageSet);
        for (String p : filterPackageSet) {
            LOGGER.info(p);
        }

        LOGGER.info("load class filter config:");
        ignoredClasses.addAll(ignoreClassesSet);
        for (String c : ignoreClassesSet) {
            LOGGER.info(c);
        }

        LOGGER.info("load ignored method:");
        ignoredMethods.addAll(ignoredMethodsSet);
        for (String m : ignoredMethodsSet) {
            LOGGER.info(m);
        }

        LOGGER.info("load std String method");
        stdStringMethods = new HashSet<>(stdStringMethodSet);
        for (String s : stdStringMethods) {
            LOGGER.info(s);
        }

        LOGGER.info("load lib interface ref info:");
        libInterfaceRef = new HashMap<>(dependInterfaces);
        for (String libInterface : dependInterfaces.keySet()) {
            LOGGER.info("load ref {}, {}", libInterface, dependInterfaces.get(libInterface));
        }

        LOGGER.info("load generic functions");
        genericFunction = new HashMap<>(genericFunctionsMap);
        for (String s : genericFunctionsMap.keySet()) {
            LOGGER.info("function {}, {}", s, genericFunctionsMap.get(s));
        }
    }

    /**
     * update subclass map
     */
    public static void updateSubclassMap() {
        for (String sub : getSuperclassMap().keySet()) {
            for (String sup : getSuperclassMap().get(sub)) {
                Set<String> subs = getSubclassMap().getOrDefault(sup, new HashSet<>());
                subs.add(sub);
                getSubclassMap().put(sup, subs);
            }
        }
    }

    private static final LinkedList<String> superClassQueue = new LinkedList<>();
    private static final Set<String> searched = new HashSet<>();

    public static int getRefCount(MethodSignature methodSignature, Value value) {
        if (methodSignature.getName().equals("<init>") || methodSignature.getName().equals("toString")){
            return 1;
        }
        ClassType classType = methodSignature.getDeclClassType();
        if (libInterfaceRef.containsKey(methodSignature.toString())) {
            return libInterfaceRef.get(methodSignature.toString());
        } else {
            int refTmp = -1;
            if (value == null || value instanceof JVirtualInvokeExpr || value instanceof JInterfaceInvokeExpr) {
                if (superclassMap.containsKey(classType.getFullyQualifiedName())) {
                    refTmp = searchSuperClass(methodSignature);
                    superClassQueue.clear();
                    searched.clear();
                }
            }
            if (refTmp != -1) {
                return refTmp;
            }
        }
        putMissingInterfaces(methodSignature);
        LOGGER.warn(String.format(
                "the ref of method %s not found in refMap, use default 0 as ref count", methodSignature));
        return 0;
    }

    public static int getRefCount(Value value) {
        if (value instanceof JDynamicInvokeExpr || isNewExpr(value) || isPackingString(value)) {
            return 1;
        }
        if (value instanceof AbstractInvokeExpr) {
            MethodSignature signature = ((AbstractInvokeExpr) value).getMethodSignature();
            return getRefCount(signature, value);
        }
        return 0;
    }

    public static int getRefCount(MethodSignature methodSignature) {
        return getRefCount(methodSignature, null);
    }

    private static int searchSuperClass(MethodSignature methodSignature) {
        String className = methodSignature.getDeclClassType().getFullyQualifiedName();
        superClassQueue.addAll(superclassMap.get(className));
        while (! superClassQueue.isEmpty()) {
            String superClass = superClassQueue.removeFirst();
            if (! searched.contains(superClass)) {
                String methodSignatureStr = methodSignature.toString();
                methodSignatureStr = methodSignatureStr.replace(className, superClass);
                if (libInterfaceRef.containsKey(methodSignatureStr)) {
                    int res = libInterfaceRef.get(methodSignatureStr);
                    libInterfaceRef.put(methodSignature.toString(), res);
                    return libInterfaceRef.get(methodSignatureStr);
                }
                if (superclassMap.containsKey(superClass)) {
                    superClassQueue.addAll(
                            superclassMap.get(superClass));
                }
                searched.add(superClass);
            }
        }
        return -1;
    }

    private static void putMissingInterfaces(MethodSignature methodSignature) {
        String className = methodSignature.getDeclClassType().getFullyQualifiedName();
        Set<String> missingMethods = missingInterfaces.getOrDefault(className, new HashSet<>());
        missingMethods.add(methodSignature.getSubSignature().toString());
        missingInterfaces.put(
                methodSignature.getDeclClassType().getFullyQualifiedName(), missingMethods);
    }

    private static boolean isNewExpr(Value value) {
        return value instanceof JNewExpr || value instanceof JNewArrayExpr || value instanceof JNewMultiArrayExpr;
    }

    private static boolean isPackingString(Value value) {
        boolean isStringConstantCast = false;
        if (value instanceof JCastExpr && value.getType() instanceof ClassType) {
            String typeString = TranslatorUtils.formatClassName(((ClassType) value.getType()).getFullyQualifiedName());
            isStringConstantCast = typeString.equals("String") && ((JCastExpr) value).getOp() instanceof StringConstant;
        }
        return value instanceof StringConstant
                || isToString(value)
                || isStringConstantCast;
    }

    private static boolean isToString(Value value) {
        if (value instanceof AbstractInstanceInvokeExpr) {
            return ((AbstractInstanceInvokeExpr) value).getMethodSignature().getName().equals("toString");
        }
        return false;
    }

    public static int getTuneLevel() {
        return tuneLevel;
    }

    public static boolean isIsMemTune() {
        return isMemTune;
    }

    public static boolean isIsHwAccTune() {
        return isHwAccTune;
    }

    public static boolean isIsRegexAcc() {
        return isRegexAcc;
    }

    public static String getCompileOption() {
        return compileOption;
    }

    public static Map<String, Set<String>> getSuperclassMap() {
        return superclassMap;
    }

    public static Map<String, Set<String>> getSubclassMap() {
        return subclassMap;
    }

    public static Map<String, Set<String>> getMissingInterfaces() {
        return missingInterfaces;
    }

    public static Map<String, String> getUdfMap() {
        return udfMap;
    }

    public static Map<String, String> getStringMap() {
        return stringMap;
    }

    public static Map<String, String> getIncludeMap() {
        return includeMap;
    }

    public static Map<String, String> getFunctionMap() {
        return functionMap;
    }

    public static Set<String> getFilterPackages() {
        return filterPackages;
    }

    public static Set<String> getIgnoredClasses() {
        return ignoredClasses;
    }

    public static Set<String> getIgnoredMethods() {
        return ignoredMethods;
    }

    public static Set<String> getStdStringMethods() {
        return stdStringMethods;
    }

    public static Map<String, String> getGenericFunction() {
        return genericFunction;
    }

    public static Map<String, Integer> getLibInterfaceRef() {
        return libInterfaceRef;
    }
}
