/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.loader;

import static com.huawei.unt.model.JavaClass.Kind.INSTANCE_METHOD_REF;
import static com.huawei.unt.model.JavaClass.Kind.STATIC_METHOD_REF;
import java.util.Arrays;
import java.util.StringJoiner;
import static sootup.core.jimple.common.constant.MethodHandle.Kind.REF_INVOKE_STATIC;

import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.type.EngineType;
import com.huawei.unt.type.UDFType;

import com.google.common.collect.ImmutableList;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JarUdfLoader use for load udf classes from jar file
 *
 * @since 2025-05-19
 */
public class JarUdfLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarUdfLoader.class);

    private static final String DES_LAMBDA = "$deserializeLambda$";
    private static final List<Type> SER_LAMBDA_PARAM =
            ImmutableList.of(JavaIdentifierFactory.getInstance().getClassType(SerializedLambda.class.getName()));

    private final JarHandler jarHandler;
    private final EngineType engineType;

    private final Set<String> requiredUdf = new HashSet<>();

    private final Map<UDFType, List<JavaClass>> classUdfMap = new HashMap<>();

    public JarUdfLoader(JarHandler jarHandler, EngineType engineType) {
        this.jarHandler = jarHandler;
        this.engineType = engineType;

        if (!TranslatorContext.getUdfMap().containsKey("required_udf")
                || "".equals(TranslatorContext.getUdfMap().get("required_udf"))) {
            EngineType.getFunctions(engineType).forEach(e -> {
                requiredUdf.add(e.getBaseClass().getName());
            });
            LOGGER.info("default translate all {} udf", engineType);
        } else {
            String requiredUdfConfig = TranslatorContext.getUdfMap().get("required_udf");
            requiredUdf.addAll(Arrays.asList(requiredUdfConfig.split(",")));
            StringJoiner loadRequiredUdfList = new StringJoiner(",");
            requiredUdf.forEach(loadRequiredUdfList::add);
            LOGGER.info("load required udf: {}", loadRequiredUdfList);
        }
    }

    public Map<UDFType, List<JavaClass>> getClassUdfMap() {
        return classUdfMap;
    }

    /**
     * load udf classes from jar
     */
    public void loadUdfClasses() {
        for (JavaSootClass javaClass : jarHandler.getAllJavaClasses()) {
            // skip ignored class
            boolean skip = false;

            for (String skipPackage : TranslatorContext.getFilterPackages()) {
                if (javaClass.getName().startsWith(skipPackage)) {
                    skip = true;
                    break;
                }
            }

            if (skip) {
                continue;
            }

            Optional<UDFType> udfType = getUDFType(javaClass, engineType);
            if (udfType.isPresent() && requiredUdf.contains(udfType.get().getBaseClass().getName())) {
                JavaClass udfClz = new JavaClass(javaClass, udfType.get());
                udfClz.addIncludes(udfType.get().getRequiredIncludes());

                if (!classUdfMap.containsKey(udfType.get())) {
                    classUdfMap.put(udfType.get(), new ArrayList<>());
                }
                classUdfMap.get(udfType.get()).add(udfClz);
            }

            List<JavaClass> lambdaClasses = null;

            try {
                lambdaClasses = loadLambdaUdfFunction(javaClass, engineType, jarHandler);
            } catch (Exception e) {
                LOGGER.warn("Load lambda classes from class {} failed, {}", javaClass.getName(), e.getMessage());
            }

            if (lambdaClasses != null && !lambdaClasses.isEmpty()) {
                for (JavaClass lambdaClass : lambdaClasses) {
                    if (requiredUdf.contains(lambdaClass.getType().getBaseClass().getName())) {
                        List<JavaClass> udfClasses = classUdfMap.getOrDefault(lambdaClass.getType(), new ArrayList<>());
                        udfClasses.add(lambdaClass);
                        classUdfMap.put(lambdaClass.getType(), udfClasses);
                    }
                }
            }
        }

        for (UDFType type : classUdfMap.keySet()) {
            LOGGER.info("Load class {} count : {}", type.getBaseClass().getSimpleName(), classUdfMap.get(type).size());
            for (JavaClass javaClass : classUdfMap.get(type)) {
                LOGGER.info(javaClass.getClassName());
            }
        }
    }

    private List<JavaClass> loadLambdaUdfFunction(JavaSootClass javaClass,
            EngineType engineType, JarHandler jarHandler) {
        List<JavaClass> lambdaUdfFunctions = new ArrayList<>();

        // scan all methods in class find lambda function
        Optional<JavaSootMethod> lambdaMethod = javaClass.getMethod(DES_LAMBDA, SER_LAMBDA_PARAM);

        lambdaMethod.ifPresent(method -> lambdaUdfFunctions.addAll(
                loadLambdaFunctionsFromBody(jarHandler, engineType, javaClass.getName(), method.getBody())));

        return lambdaUdfFunctions;
    }

    private Set<JavaClass> loadLambdaFunctionsFromBody(JarHandler jarHandler,
            EngineType engineType, String className, Body body) {
        Map<Local, UDFType> udfTypeMap = new HashMap<>();

        for (Local local : body.getLocals()) {
            if (local.getType() instanceof ClassType) {
                TranslatorUtils.getUdfType((ClassType) local.getType(), engineType)
                        .ifPresent(udfType -> udfTypeMap.put(local, udfType));
            }
        }

        Set<JavaClass> lambdaFunctions = new HashSet<>();

        for (Stmt stmt : body.getStmts()) {
            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getLeftOp() instanceof Local
                    && udfTypeMap.containsKey((Local) ((JAssignStmt) stmt).getLeftOp())
                    && ((JAssignStmt) stmt).getRightOp() instanceof JDynamicInvokeExpr) {
                JDynamicInvokeExpr invokeExpr = (JDynamicInvokeExpr) ((JAssignStmt) stmt).getRightOp();
                UDFType udfType = udfTypeMap.get((Local) ((JAssignStmt) stmt).getLeftOp());
                JavaClass lambdaClass = getLambdaClass(invokeExpr, udfType, className);
                lambdaClass.addIncludes(udfType.getRequiredIncludes());
                lambdaFunctions.add(lambdaClass);
            }
        }

        return lambdaFunctions;
    }

    private JavaClass getLambdaClass(JDynamicInvokeExpr invokeExpr, UDFType udfType, String className) {
        Optional<JavaClass> lambdaClass = Optional.empty();
        MethodHandle methodHandle = null;

        if (invokeExpr.getBootstrapArgCount() > 2
                && invokeExpr.getBootstrapArg(1) instanceof MethodHandle) {
            methodHandle = (MethodHandle) invokeExpr.getBootstrapArg(1);
        }

        if (methodHandle != null && methodHandle.isMethodRef() && invokeExpr.getArgs().isEmpty()) {
            MethodSignature methodSignature = (MethodSignature) methodHandle.getReferenceSignature();
            if (methodHandle.getKind().equals(REF_INVOKE_STATIC)) {
                lambdaClass = methodSignature.getDeclClassType().getFullyQualifiedName().equals(className) ?
                        getSimpleLambdaClass(methodSignature, udfType) : getStaticMethodRefClass(methodSignature, udfType);
            } else {
                lambdaClass = getMethodRefClass(methodSignature,udfType);
            }
        }

        if (lambdaClass.isPresent()) {
            return lambdaClass.get();
        } else {
            throw new TranslatorException("unsupported flink udf with dynamic invoke \n" + invokeExpr);
        }
    }

    private Optional<JavaClass> getMethodRefClass(MethodSignature methodSignature, UDFType udfType) {
        return Optional.of(new JavaClass(methodSignature, udfType, INSTANCE_METHOD_REF));
    }

    private Optional<JavaClass> getStaticMethodRefClass(MethodSignature methodSignature, UDFType udfType) {
        return Optional.of(new JavaClass(methodSignature, udfType, STATIC_METHOD_REF));
    }

    private Optional<JavaClass> getSimpleLambdaClass(MethodSignature methodSignature, UDFType udfType) {

        List<String> paramTypes = methodSignature.getParameterTypes().stream()
                .map(Type::toString).collect(Collectors.toList());

        String className = methodSignature.getDeclClassType().getFullyQualifiedName();
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                className,
                methodSignature.getName(),
                methodSignature.getType().toString(),
                paramTypes);

        String outputClassName = TranslatorUtils.formatLambdaUdfClassName(methodSignature, udfType);

        if (method.isPresent()) {
            JavaClass lambdaClass = new JavaClass(outputClassName, udfType, method.get());
            lambdaClass.addIncludes(udfType.getRequiredIncludes());
            return Optional.of(lambdaClass);
        }
        return Optional.empty();
    }

    private Optional<UDFType> getUDFType(JavaSootClass javaClass, EngineType type) {
        for (UDFType udfType : EngineType.getFunctions(type)) {
            if (jarHandler.isSubClass(javaClass.getName(),
                    JavaIdentifierFactory.getInstance().getClassType(udfType.getBaseClass().getName()))) {
                return Optional.of(udfType);
            }
        }
        return Optional.empty();
    }
}
