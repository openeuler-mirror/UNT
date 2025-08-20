/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;

import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaSootMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * UDFType interface
 *
 * @since 2025-05-19
 */
public interface UDFType {
    /**
     * get UDF base Class
     *
     * @return udf base class
     */
    default Class<?> getBaseClass() {
        return Object.class;
    }

    /**
     * Return cpp file string
     *
     * @param className udf class name
     * @return cpp file code string
     */
    default String getCppFileString(String className) {
        return "";
    }

    /**
     * get so file name prefix
     *
     * @return so file name prefix
     */
    default String getSoPrefix() {
        return "";
    }

    /**
     * check if the method is udf interface method
     *
     * @param method method
     * @return udf method or not
     */
    default boolean isUdfFunction(JavaSootMethod method) {
        return false;
    }

    /**
     * print declare method
     *
     * @param method method
     * @return declare method string
     */
    default String printDeclareMethod(JavaSootMethod method) {
        return TranslatorUtils.printDeclareMethod(method);
    }

    /**
     * print head and params if it is udf function
     *
     * @param methodContext methodContext
     * @return head and params
     */
    default String printHeadAndParams(MethodContext methodContext) {
        return TranslatorUtils.printHeadAndParams(methodContext);
    }

    /**
     * Return required includes
     *
     * @return required includes
     */
    default Set<ClassType> getRequiredIncludes() {
        return Collections.emptySet();
    }

    /**
     * print lambda declare function
     *
     * @return lambda declare function
     */
    default String printLambdaDeclare() {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }

    /**
     * print lambda head and param
     *
     * @param methodContext methodContext
     * @return lambda head and params code string
     */
    default String printLambdaHeadAndParams(MethodContext methodContext) {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }

    /**
     * print method ref head and params
     *
     * @param className className
     * @param paramTypes paramTypes
     * @return method ref head and params
     */
    default String printMethodRefHeadAndParams(String className, List<Type> paramTypes) {
        throw new TranslatorException("This kind of udf does not support method ref function");
    }

    /**
     * check if the return of Lambda is referenced
     *
     * @return boolean
     */
    default boolean refLambdaReturn() {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }
}
