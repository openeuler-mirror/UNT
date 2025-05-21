package com.huawei.unt.translator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class TranslatorContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorContext.class);

    public static final String NEW_LINE = "\n";
    public static final String INIT_FUNCTION_NAME = "<init>";
    public static final String STATIC_INIT_FUNCTION_NAME = "<clinit>";
    public static final String TAB = "    ";
    public static final String SIMPLE_PUT_REF = TAB + "%1$s->putRefCount();" + NEW_LINE;
    public static final String UNKNOWN_PUT_REF = TAB + "if (%1$s != nullptr){" + NEW_LINE +
            TAB + TAB + "%1$s->putRefCount();" + NEW_LINE +
            TAB + "}" + NEW_LINE;
    public static final String CIRCLE_PUT_REF = TAB + "if (%1$s != nullptr){" + NEW_LINE +
            TAB + TAB + "%1$s->putRefCount();" + NEW_LINE +
            TAB + TAB + "%1$s = nullptr;" + NEW_LINE +
            TAB + "}" + NEW_LINE;
    public static final String GET_REF = TAB + "if (%1$s != nullptr){" + NEW_LINE +
            TAB + TAB + "%1$s->getRefCount();" + NEW_LINE +
            TAB + "}" + NEW_LINE;
    public static final String MAKE_NULL = TAB + "%s = nullptr;" + NEW_LINE;
    public static final String RET_ASSIGN = TAB + "ret = %s;" + NEW_LINE;
    public static final String GOTO_FREE = TAB + "goto free;" + NEW_LINE;
    public static final String RETURN_RET = TAB + "return ret;" + NEW_LINE;
    public static final String CLEAR = TAB + "%s->clear();" + NEW_LINE;
    public static final String TMP_OBJ_DECLARE = TAB + "Object *tmpObj = nullptr;" + NEW_LINE;
    public static final String TMP_OBJ_ASSIGN = TAB + "Object *tmpObj = %s;" + NEW_LINE;
    public static final String TMP_OBJ_FREE = String.format(CIRCLE_PUT_REF, "tmpObj");
    public static final String OLD_VAR_DECLARE = TAB + "%1$sold%2$S;" + NEW_LINE;
    public static final String OLD_VAR_ASSIGN = TAB + "old%1$S = %1$s;" + NEW_LINE;;
    public static final String OLD_VAR_PUT = TAB + "if (old%1$S != nullptr){" + NEW_LINE +
            TAB + TAB + "old%1$S->putRefCount();" + NEW_LINE +
            TAB + "}" + NEW_LINE;
    // todo
    public static int  ARRAY_LIB_TYPE = 0;
    public static int TUNELEVEL;
    public static boolean ISMEMTUNE;
    public static boolean ISHWACCTUNE;
    public static boolean ISREGEXACC;
    public static String COMPILEOPTION;
    public static Map<String, Set<String>> SUPERCLASS_MAP = new HashMap<>();
    public static Map<String, Set<String>> SUBCLASS_MAP = new HashMap<>();
    public static Map<String, Set<String>> MISSING_INTERFACES = new HashMap<>();
    public static Map<String, String> UDF_MAP;
    public static Map<String, String> CLASS_MAP;
    public static Map<String, String> INCLUDE_MAP;
    public static Map<String, String> FUNCTION_MAP;
    public static Map<String, Integer> LIB_INTERFACE_REF;
    public static Set<String> FILTER_PACKAGES;
    public static Set<String> IGNORED_CLASSES;
    public static Set<String> IGNORED_METHODS;
    public static Set<String> PRIMARY_TYPES;
    public static Set<String> STD_STRING_METHODS;
    public static Set<String> GENERIC_FUNCTION;

    // todo: make it configurable
    public static final int MAX_CLASS_DEPTH = 100;
    public static final int MAX_CLASS_COUNT = 100;
    public static final int MAX_FUNCTION_SIZE = 1000;

    public static void init(String configDir) {
        // make it log info
        GENERIC_FUNCTION = new HashSet<>();
        GENERIC_FUNCTION.add("FilterFunction");
        GENERIC_FUNCTION.add("RichParallelSourceFunction");
        GENERIC_FUNCTION.add("FlatMapFunction");
        GENERIC_FUNCTION.add("RichFlatMapFunction");
        GENERIC_FUNCTION.add("MapFunction");
        GENERIC_FUNCTION.add("ReduceFunction");
        GENERIC_FUNCTION.add("KeySelect");

        LOGGER.info("Init TranslatorContext");

        String udfConfigDir = (configDir.endsWith(File.separator) ? configDir : configDir + File.separator)
                +"udf_tune.properties";
        LOGGER.info("load properties: {}", udfConfigDir);
        Properties udfProperties = new Properties();

        try{
            udfProperties.load(Files.newInputStream(Paths.get(udfConfigDir)));
            LOGGER.info("load udf config");
            UDF_MAP = new HashMap<String, String>();
            for (Object key : udfProperties.keySet()) {
                LOGGER.info("load [{}] = [{}]", key, udfProperties.getProperty((String) key));
                UDF_MAP.put((String)key,udfProperties.getProperty((String)key));
            }
        } catch (IOException e) {
            throw new TranslatorException("Load udf_config files failed: " + e.getMessage());
        }

        if (!UDF_MAP.containsKey("base.dir")){
            throw new TranslatorException("base.dir is not set");
        }
        if (!UDF_MAP.containsKey("tune_level")){
            LOGGER.info("tune_level is not configured, use default tune_level 0");
            TUNELEVEL = 0;
        }else {
            int tuneLevel = Integer.valueOf(UDF_MAP.get("tune_level"));
            if (tuneLevel > 4 || tuneLevel < 0){
                LOGGER.info("tune_level is incorrectly configured, use default tune_level 0");
                TUNELEVEL = 0;
            }else {
                LOGGER.info("using tune_level " + tuneLevel);
                TUNELEVEL = tuneLevel;
            }
        }
        if ((TUNELEVEL & 2) != 0){
            ISMEMTUNE = true;
            LOGGER.info("Enabling Memory Optimization");
        }else {
            ISMEMTUNE = false;
            LOGGER.info("Use the default memory policy.");
        }

        if ((TUNELEVEL & 1) != 0){
            ISHWACCTUNE = true;
            LOGGER.info("Enabling Hardware Acceleration Optimization");
        }else {
            ISHWACCTUNE = false;
            LOGGER.info("Disabling Hardware Acceleration Optimization.");
        }

        if (ISHWACCTUNE && UDF_MAP.getOrDefault("regex_lib_type","0").equals("1")){
            ISREGEXACC = true;
            LOGGER.info("Enabling Regex acc.");
        }else {
            ISREGEXACC = false;
            LOGGER.info("Disabling Regex acc.");
        }

        if (!UDF_MAP.containsKey("compile_option")||UDF_MAP.get("compile_option").equals("")){
            COMPILEOPTION = "-o3 -std=c++17 -fPIC";
            LOGGER.info("use default compile_option: -o3 -std=c++17 -fPIC");
        }else {
            COMPILEOPTION = UDF_MAP.get("compile_option");
            LOGGER.info("use compile_option: " + COMPILEOPTION);
        }

        configDir = UDF_MAP.get("base.dir").endsWith(File.separator)?UDF_MAP.get("base.dir") : UDF_MAP.get("base.dir") + File.separator;

        LOGGER.info("load conf base");

        String classProfile = configDir + "conf" + File.separator + "depend_class.properties";
        String functionProfile = configDir + "conf" + File.separator + "function.properties";
        String includeProfile = configDir + "conf" + File.separator + "depend_include.properties";
        String ignorePackageProfile = configDir + "conf" + File.separator + "ignoredPackage";
        String ignoreClassProfile = configDir + "conf" + File.separator + "ignoredClasses";
        String externPrimaryTypesProfile = configDir + "conf" + File.separator + "externPrimaryTypes";
        String ignoredMethodsProfile = configDir + "conf" + File.separator + "ignoredMethods";
        String stdStringMethodsProfile = configDir + "conf" + File.separator + "stdStringMethods";
        String libInterfaceRefsInfo = configDir + "conf" + File.separator + "depend_interface.config";

        Properties classProperties = new Properties();
        Properties functionProperties = new Properties();
        Properties includeProperties = new Properties();
        Set<String> filterPackages = new HashSet<>();
        Set<String> ignoreClasses = new HashSet<>();
        Set<String> externPrimaryTypes = new HashSet<>();
        Set<String> ignoredMethods = new HashSet<>();
        Set<String> stdStringMethods = new HashSet<>();
        Map<String, Integer> libInterfaceRefs = new HashMap<>();

        try {
            classProperties.load(Files.newInputStream(Paths.get(classProfile)));

            functionProperties.load((Files.newInputStream(Paths.get(functionProfile))));
            includeProperties.load((Files.newInputStream(Paths.get(includeProfile))));

            BufferedReader ignoredPackageReader = Files.newBufferedReader(Paths.get(ignorePackageProfile));
            BufferedReader ignoredClassReader = Files.newBufferedReader(Paths.get(ignoreClassProfile));
            BufferedReader externPrimaryTypesReader = Files.newBufferedReader(Paths.get(externPrimaryTypesProfile));
            BufferedReader ignoredMethodReader = Files.newBufferedReader(Paths.get(ignoredMethodsProfile));
            BufferedReader stdStringMethodReader = Files.newBufferedReader(Paths.get(stdStringMethodsProfile));
            BufferedReader libInterFaceRefsReader = Files.newBufferedReader(Paths.get(libInterfaceRefsInfo));

            String ignorePackage;
            while ((ignorePackage = ignoredPackageReader.readLine()) != null) {
                filterPackages.add(ignorePackage.trim());
            }

            String ignoredClass;
            while ((ignoredClass = ignoredClassReader.readLine()) != null) {
                ignoreClasses.add(ignoredClass.trim());
            }

            String externPrimaryType;
            while ((externPrimaryType = externPrimaryTypesReader.readLine()) != null) {
                externPrimaryTypes.add(externPrimaryType.trim());
            }

            String ignoredMethod;
            while ((ignoredMethod = ignoredMethodReader.readLine()) != null) {
                ignoredMethods.add(ignoredMethod.trim());
            }

            String stdStringMethod;
            while ((stdStringMethod = stdStringMethodReader.readLine()) != null) {
                stdStringMethods.add(stdStringMethod);
            }

            String interfaceRef;
            while((interfaceRef = libInterFaceRefsReader.readLine()) != null) {
                String[] ref = interfaceRef.trim().split(", ");
                libInterfaceRefs.put(ref[0].trim(), Integer.valueOf(ref[1].trim()));
            }
        } catch (IOException e) {
            throw new TranslatorException("Load config files failed: " + e.getMessage());
        }

        LOGGER.info("load class config:");
        CLASS_MAP = new HashMap<>();
        for (Object key : classProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, classProperties.getProperty((String) key));
            CLASS_MAP.put((String) key, classProperties.getProperty((String) key));
        }

        LOGGER.info("load function config:");
        FUNCTION_MAP = new HashMap<>();
        for (Object key : functionProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, functionProperties.getProperty((String) key));
            FUNCTION_MAP.put((String) key, functionProperties.getProperty((String) key));
        }

        LOGGER.info("load include config:");
        INCLUDE_MAP = new HashMap<>();
        for (Object key : includeProperties.keySet()) {
            LOGGER.info("load [{}] = [{}]", key, includeProperties.getProperty((String) key));
            INCLUDE_MAP.put((String) key, includeProperties.getProperty((String) key));
        }

        LOGGER.info("load package filter config:");
        FILTER_PACKAGES = filterPackages;
        for (String p : filterPackages) {
            LOGGER.info(p);
        }

        LOGGER.info("load class filter config:");
        IGNORED_CLASSES = ignoreClasses;
        for (String c : ignoreClasses) {
            LOGGER.info(c);
        }

        LOGGER.info("load extern primary type:");
        PRIMARY_TYPES = externPrimaryTypes;
        for (String e : externPrimaryTypes) {
            LOGGER.info(e);
        }

        LOGGER.info("load ignored method:");
        IGNORED_METHODS = ignoredMethods;
        for (String m : ignoredMethods) {
            LOGGER.info(m);
        }

        LOGGER.info("load std String method");
        STD_STRING_METHODS = stdStringMethods;
        for (String s : stdStringMethods) {
            LOGGER.info(s);
        }

        LOGGER.info("load lib interface ref info:");
        LIB_INTERFACE_REF = libInterfaceRefs;
        for (String libInterface : libInterfaceRefs.keySet()) {
            LOGGER.info("load ref {}, {}", libInterface, libInterfaceRefs.get(libInterface));
        }
    }

    private TranslatorContext() {}

    public static void updateSubclassMap() {
        for (String sub : SUPERCLASS_MAP.keySet()) {
            for (String sup : SUPERCLASS_MAP.get(sub)) {
                Set<String> subs = SUBCLASS_MAP.getOrDefault(sup, new HashSet<>());
                subs.add(sub);
                SUBCLASS_MAP.put(sup, subs);
            }
        }
    }
}
