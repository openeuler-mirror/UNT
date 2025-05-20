package com.huawei.unt.type;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;

import java.util.Collections;
import java.util.Set;

public interface UDFType {
    default Class<?> getBaseClass() {
        return null;
    }

    default String getCppFileString(String className) {
        return null;
    }

    default String getSoPrefix() {
        return null;
    }

    default boolean isUdfFunction(JavaSootMethod method) {
        return false;
    }

    default String printDeclareMethod(JavaSootMethod method) {
        return TranslatorUtils.printDeclareMethod(method);
    }

    default String printHeadAndParams(MethodContext methodContext) {
        return TranslatorUtils.printHeadAndParams(methodContext);
    }

    default Set<ClassType> getRequiredIncludes() {
        return Collections.emptySet();
    }

    default String printLambdaDeclare() {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }

    default String printLambdaHeadAndParams(MethodContext methodContext) {
        throw new TranslatorException("This kind of udf does not support lambda function");
    }
}
