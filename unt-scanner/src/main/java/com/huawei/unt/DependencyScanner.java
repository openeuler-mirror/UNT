/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * DependencyScanner use for scan all dependency class used in udf
 *
 * @since 2025-05-19
 */
public class DependencyScanner {
    private static final String NEW_LINE = System.lineSeparator();
    private static final String TAB = "    ";
    private static final String ARRAY_TYPE = "Array";
    private static final String FLINK_PREFIX = "org.apache.flink";

    private static JarHandler jarHandler;
    private static Set<String> depClasses;
    private static Set<String> missingClasses;
    private static Set<String> flinkClasses;
    private static Map<String, Set<String>> missingFields;
    private static Map<String, Set<String>> missingMethods;
    private static Map<String, Set<String>> nativeMethods;

    private static Queue<String> queue = new ArrayDeque<>();

    /**
     * scan dependency of udf in jar, write to file
     *
     * @param jarHandler jarHandler
     * @param writer fileWriter
     * @throws IOException IOException
     */
    public static void dependencyScan(JarHandler jarHandler, FileWriter writer) throws IOException {
        DependencyScanner.jarHandler = jarHandler;
        Set<JavaClass> udfClasses = jarHandler.loadUdfClasses();
        Set<String> udfClassNames = udfClasses.stream().map(JavaClass::getClassName).collect(Collectors.toSet());

        if (udfClasses.isEmpty()) {
            writer.write("No flink udf found in jar!");
            return;
        }

        for (JavaClass udfClass : udfClasses) {
            writer.write("====================================================" + NEW_LINE);
            writer.write("Find udf class: [" + udfClass.getClassName() + "]" + NEW_LINE);
            writer.write("UDF extends:" + NEW_LINE);
            for (ClassType classType : udfClass.getSupperClasses()) {
                if (jarHandler.isUdfType(classType.getFullyQualifiedName())) {
                    writer.write(TAB + formatType(classType) + "" + NEW_LINE);
                }
            }

            depClasses = new HashSet<>(udfClassNames);
            missingClasses = new HashSet<>();
            flinkClasses = new HashSet<>();
            missingFields = new HashMap<>();
            missingMethods = new HashMap<>();
            nativeMethods = new HashMap<>();
            queue = new ArrayDeque<>();
            queue.add(udfClass.getClassName());

            while (!queue.isEmpty()) {
                JavaClass clz = DependencyScanner.jarHandler.getJavaClassByName(queue.poll());

                try {
                    // scan supper classes
                    for (ClassType classType : clz.getSupperClasses()) {
                        dealWithClass(classType.getFullyQualifiedName());
                    }

                    // scan fields
                    for (JavaSootField field : clz.getFields()) {
                        if (field.getType() instanceof ClassType) {
                            ClassType classType = (ClassType) field.getType();
                            String className = classType.getFullyQualifiedName();
                            dealWithClass(className);
                        }
                    }

                    // scan methods
                    Set<JavaSootMethod> methods = clz.getMethods();

                    for (JavaSootMethod method : methods) {
                        if (method.isNative()) {
                            depClasses.add(clz.getClassName());
                            if (!nativeMethods.containsKey(clz.getClassName())) {
                                nativeMethods.put(clz.getClassName(), new HashSet<>());
                            }
                            nativeMethods.get(clz.getClassName()).add(printMethodSignature(method.getSignature()));
                            continue;
                        }

                        // scan returnType&paramTypes
                        if (method.getReturnType() instanceof ClassType) {
                            dealWithClass(((ClassType) (method.getReturnType())).getFullyQualifiedName());
                        }

                        for (Type paramType : method.getParameterTypes()) {
                            if (paramType instanceof ClassType) {
                                dealWithClass(((ClassType) paramType).getFullyQualifiedName());
                            }
                        }

                        if (method.isConcrete()) {
                            Body body = method.getBody();
                            // scan locals
                            for (Local local : body.getLocals()) {
                                if (local.getType() instanceof ClassType) {
                                    dealWithClass(((ClassType) local.getType()).getFullyQualifiedName());
                                }
                            }
                            // scan body
                            for (Stmt stmt : body.getStmts()) {
                                dealWithStmt(stmt);
                            }
                        }
                    }
                } catch (Exception e) {
                    writer.write("Analyze class " + clz.getClassName() + " failed, " + e.getMessage() + ""
                            + NEW_LINE);
                }
            }

            // print scan result
            printResult(writer);
            writer.write("====================================================" + NEW_LINE + NEW_LINE);
        }
    }

    private static void printResult(FileWriter writer) throws IOException {
        if (!flinkClasses.isEmpty()) {
            writer.write("Required Flink classes:" + NEW_LINE);

            for (String clz : flinkClasses) {
                writer.write(TAB + clz + "" + NEW_LINE);
            }
        }

        if (!nativeMethods.isEmpty()) {
            writer.write("Has native methods:" + NEW_LINE);

            for (Map.Entry<String, Set<String>> entry : nativeMethods.entrySet()) {
                for (String method : entry.getValue()) {
                    writer.write(TAB + entry.getKey() + "::" + method + "" + NEW_LINE);
                }
            }
        }

        if (!missingClasses.isEmpty()) {
            writer.write("Missing classes:" + NEW_LINE);
            for (String missingClz : missingClasses) {
                writer.write(missingClz + "" + NEW_LINE);
                if (missingFields.containsKey(missingClz) && !missingFields.get(missingClz).isEmpty()) {
                    writer.write("Fields:" + "" + NEW_LINE);
                    for (String field : missingFields.get(missingClz)) {
                        writer.write(TAB + field + "" + NEW_LINE);
                    }
                }
                if (missingMethods.containsKey(missingClz) && !missingMethods.get(missingClz).isEmpty()) {
                    writer.write("Methods:" + "" + NEW_LINE);
                    for (String method : missingMethods.get(missingClz)) {
                        writer.write(TAB + method + "" + NEW_LINE);
                    }
                }
            }
        }
    }


    private static void dealWithStmt(Stmt stmt) {
        if (stmt instanceof JInvokeStmt && ((JInvokeStmt) stmt).getInvokeExpr().isPresent()) {
            JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
            dealWithMethod(invokeStmt.getInvokeExpr().get().getMethodSignature());
        }

        if (stmt instanceof JAssignStmt) {
            Value value = ((JAssignStmt) stmt).getRightOp();
            if (value instanceof AbstractInvokeExpr) {
                dealWithMethod(((AbstractInvokeExpr) value).getMethodSignature());
            }
            if (value instanceof JFieldRef) {
                dealWithField(((JFieldRef) value).getFieldSignature());
            }
        }
    }

    private static void dealWithClass(String className) {
        if (depClasses.contains(className)) {
            return;
        }

        if (className.startsWith(FLINK_PREFIX)) {
            flinkClasses.add(className);
            return;
        }
        if (jarHandler.containsClass(className)) {
            depClasses.add(className);
            queue.add(className);
            return;
        }
        if (!missingClasses.contains(className)) {
            missingClasses.add(className);
            missingFields.put(className, new HashSet<>());
            missingMethods.put(className, new HashSet<>());
        }
    }

    private static void dealWithField(FieldSignature field) {
        String className = field.getDeclClassType().getFullyQualifiedName();
        dealWithClass(className);
        if (missingClasses.contains(className)) {
            missingFields.get(className).add(printFieldSignature(field));
        }
    }

    private static void dealWithMethod(MethodSignature method) {
        String className = method.getDeclClassType().getFullyQualifiedName();

        dealWithClass(className);

        if (missingClasses.contains(className)) {
            missingMethods.get(className).add(printMethod(method));
        }
    }

    private static String printMethod(MethodSignature signature) {
        String returnType = formatType(signature.getType());

        StringJoiner joiner = new StringJoiner(", ");
        for (Type type : signature.getParameterTypes()) {
            joiner.add(formatType(type));
        }

        return returnType + " " + signature.getName() + "(" + joiner + ")";
    }

    private static String printFieldSignature(FieldSignature signature) {
        String type = formatType(signature.getType());
        return type + " " + signature.getName();
    }

    private static String printMethodSignature(MethodSignature signature) {
        String returnType = formatType(signature.getType());

        StringJoiner joiner = new StringJoiner(", ");
        for (Type type : signature.getParameterTypes()) {
            joiner.add(formatType(type));
        }

        return returnType + " " + signature.getName() + "(" + joiner + ")";
    }

    private static String formatType(Type type) {
        if (type instanceof ArrayType) {
            return ARRAY_TYPE;
        }

        return TypePrinter.getTypeString(type);
    }
}
