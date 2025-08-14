/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.loader;

import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.TranslatorContext;
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
import sootup.core.signatures.MethodSubSignature;
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

    private final Map<UDFType, List<JavaClass>> classUdfMap = new HashMap<>();

    public JarUdfLoader(JarHandler jarHandler, EngineType engineType) {
        this.jarHandler = jarHandler;
        this.engineType = engineType;
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

            if (udfType.isPresent()) {
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
                    if (!classUdfMap.containsKey(lambdaClass.getType())) {
                        classUdfMap.put(lambdaClass.getType(), new ArrayList<>());
                    }
                    classUdfMap.get(lambdaClass.getType()).add(lambdaClass);
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
                MethodHandle methodHandle;
                if (invokeExpr.getBootstrapArgCount() > 2
                        && invokeExpr.getBootstrapArg(1) instanceof MethodHandle) {
                    methodHandle = (MethodHandle) invokeExpr.getBootstrapArg(1);
                } else {
                    continue;
                }

                MethodSubSignature signature;
                if (methodHandle.getReferenceSignature().getSubSignature() instanceof MethodSubSignature) {
                    signature = (MethodSubSignature) methodHandle.getReferenceSignature().getSubSignature();
                } else {
                    continue;
                }

                List<String> paramTypes = signature.getParameterTypes().stream()
                        .map(Type::toString).collect(Collectors.toList());

                Optional<JavaSootMethod> method = jarHandler.tryGetMethod(className, signature.getName(),
                        signature.getType().toString(), paramTypes);

                String outputClassName = className + "$" + signature.getName();

                if (method.isPresent()) {
                    UDFType udfType = udfTypeMap.get((Local) ((JAssignStmt) stmt).getLeftOp());
                    JavaClass lambdaClass = new JavaClass(outputClassName, udfType, method.get());
                    lambdaClass.addIncludes(udfType.getRequiredIncludes());
                    lambdaFunctions.add(lambdaClass);
                }
            }
        }

        return lambdaFunctions;
    }

    private Optional<UDFType> getUDFType(JavaSootClass javaClass, EngineType type) {
        for (UDFType udfType : EngineType.getFunctions(type)) {
            udfType.getBaseClass();
            if (jarHandler.isSubClass(javaClass.getName(),
                    JavaIdentifierFactory.getInstance().getClassType(udfType.getBaseClass().getName()))) {
                return Optional.of(udfType);
            }
        }
        return Optional.empty();
    }
}
