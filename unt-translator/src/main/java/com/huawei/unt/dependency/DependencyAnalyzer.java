/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.dependency;

import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.loader.LoaderException;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.NoneUDF;

import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractStmtVisitor;
import sootup.core.jimple.visitor.AbstractValueVisitor;
import sootup.core.model.Body;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Analyze dependency
 *
 * @since 2025-05-19
 */
public class DependencyAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyAnalyzer.class);
    private final JarHandler jarHandler;

    private final Map<ClassType, JavaClass> allClasses = new HashMap<>();
    private final Queue<JavaClass> classQueue = new ArrayDeque<>();

    private final List<JavaClass> udfClasses = new ArrayList<>();

    public DependencyAnalyzer(JarHandler jarHandler, List<JavaClass> allClassUDFs) {
        this.jarHandler = jarHandler;
        for (JavaClass javaClass : allClassUDFs) {
            allClasses.put(JavaIdentifierFactory.getInstance().getClassType(javaClass.getClassName()), javaClass);
            classQueue.add(javaClass);
            udfClasses.add(javaClass);
        }
    }

    /**
     * Analyze loop include
     */
    public void loopIncludeAnalyzer() {
        for (JavaClass allDependencyClassOuter : allClasses.values()) {
            Set<ClassType> includeOuter = allDependencyClassOuter.getIncludes();
            for (JavaClass allDependencyClassInner : allClasses.values()) {
                if (allDependencyClassOuter == allDependencyClassInner) {
                    continue;
                }
                String classOuter = allDependencyClassOuter.getClassName();
                String classInner = allDependencyClassInner.getClassName();
                Set<ClassType> includeInner = allDependencyClassInner.getIncludes();
                for (ClassType classType : includeInner) {
                    if (classOuter.equals(classType.getFullyQualifiedName())) {
                        for (ClassType include : includeOuter) {
                            if (include.getFullyQualifiedName().equals(classInner)) {
                                // loopInclude
                                allDependencyClassInner.addLoopInclude(classType);
                                allDependencyClassOuter.addLoopInclude(include);
                                LOGGER.info("{} and {} loop include", allDependencyClassInner.getClassName(),
                                        allDependencyClassOuter);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addJsonConstructorFlag() {
        Map<ClassType, JavaClass> addJsonConstructorMap = new HashMap<>();
        Queue<JavaClass> addJsonConstructorQueue = new ArrayDeque<>();
        for (JavaClass javaClass : udfClasses) {
            addJsonConstructorMap.put(JavaIdentifierFactory.getInstance().getClassType(
                    javaClass.getClassName()), javaClass);
            addJsonConstructorQueue.add(javaClass);
        }
        while (!addJsonConstructorQueue.isEmpty()) {
            JavaClass javaClass = addJsonConstructorQueue.poll();
            boolean hasObjectField = false;

            HashSet<ClassType> needToJsonConstruct = new HashSet<>();

            Set<ClassType> supperClasses = javaClass.getSupperClasses().stream()
                    .filter(c -> !TranslatorContext.getStringMap().containsKey(c.getFullyQualifiedName()))
                    .collect(Collectors.toSet());

            needToJsonConstruct.addAll(supperClasses);

            Set<JavaSootField> fields = javaClass.getFields();
            for (JavaSootField field : fields) {
                Type fieldType = field.getType();

                if (fieldType instanceof ClassType) {
                    ClassType fieldClassType = (ClassType) fieldType;
                    if ("java.lang.Object".equals(fieldClassType.getFullyQualifiedName())) {
                        hasObjectField = true;
                    }
                    if (TranslatorContext.getStringMap().containsKey(fieldClassType.getFullyQualifiedName())) {
                        continue;
                    }
                    needToJsonConstruct.add(fieldClassType);
                }
            }

            Set<ClassType> newJsonConstruct = needToJsonConstruct.stream()
                    .filter(c -> !addJsonConstructorMap.containsKey(c))
                    .collect(Collectors.toSet());

            for (ClassType classType : newJsonConstruct) {
                try {
                    JavaClass newJsonjavaClass = allClasses.get(classType);
                    addJsonConstructorMap.put(JavaIdentifierFactory.getInstance().getClassType(
                            newJsonjavaClass.getClassName()), newJsonjavaClass);
                    addJsonConstructorQueue.add(newJsonjavaClass);
                } catch (Exception e) {
                    LOGGER.error("fieldClassType {} not found in allNeedTransClasses ", classType.getClassName());
                }
            }

            if (hasObjectField) {
                javaClass.setHasObjectField();
            }
        }
        for (JavaClass value : addJsonConstructorMap.values()) {
            value.setJsonConstructor();
            LOGGER.info("{} need to construct using json", value.getClassName());
        }
    }

    /**
     * Get all dependency classes
     *
     * @return all dependency classes
     */
    public Collection<JavaClass> getAllDependencyClasses() {
        while (!classQueue.isEmpty()) {
            JavaClass javaClass = classQueue.poll();
            LOGGER.info("Start analyze class: {}", javaClass.getClassName());
            boolean isMissingClass = false;
            Map<ClassType, JavaClass> newFoundClasses = new HashMap<>();
            Set<ClassType> missingClasses = new HashSet<>();

            ClassType thisClassType = JavaIdentifierFactory.getInstance().getClassType(javaClass.getClassName());

            DependencyStmtVisitor stmtVisitor = new DependencyStmtVisitor();

            Set<ClassType> dependencies = new HashSet<>(javaClass.getSupperClasses());

            for (JavaSootField field : javaClass.getFields()) {
                if (field.getType() instanceof ClassType) {
                    dependencies.add((ClassType) field.getType());
                }
                if (field.getType() instanceof ArrayType) {
                    javaClass.setHasArray();
                }
            }

            for (JavaSootMethod method : javaClass.getMethods()) {
                LOGGER.info("Start analyze method: {}", method);
                if (method.isMain(JavaIdentifierFactory.getInstance())) {
                    continue;
                }
                if (TranslatorContext.getIgnoredMethods().contains(method.getSignature().toString())
                        || !method.hasBody()) {
                    // abstract method && ignored method analyze param
                    for (Type paramType : method.getParameterTypes()) {
                        if (paramType instanceof ClassType) {
                            dependencies.add((ClassType) paramType);
                        }
                    }
                    continue;
                }

                Body body = method.getBody();

                for (Local local : body.getLocals()) {
                    if (local.getType() instanceof ClassType) {
                        dependencies.add((ClassType) local.getType());
                    }
                    if (local.getType() instanceof ArrayType) {
                        javaClass.setHasArray();
                    }
                }

                for (Stmt stmt : body.getStmts()) {
                    stmt.accept(stmtVisitor);
                }
            }

            dependencies.addAll(stmtVisitor.getClasses());
            stmtVisitor.clear();

            dependencies.remove(thisClassType);

            if (javaClass.getRefMethod() != null) {
                dependencies.add(javaClass.getRefMethod().getDeclClassType());
            }

            Set<ClassType> includes = dependencies.stream()
                    .filter(c -> !TranslatorContext.getIgnoredClasses().contains(c.getFullyQualifiedName()))
                    .collect(Collectors.toSet());

            javaClass.addIncludes(includes);

            Set<ClassType> newDependencies = includes.stream()
                    .filter(c -> !allClasses.containsKey(c))
                    .filter(c -> !TranslatorContext.getStringMap().containsKey(c.getFullyQualifiedName()))
                    .collect(Collectors.toSet());

            for (ClassType classType : newDependencies) {
                JavaClass newClass;
                try {
                    newClass = jarHandler.getJavaClass(classType, NoneUDF.INSTANCE);
                } catch (LoaderException e) {
                    isMissingClass = true;
                    missingClasses.add(classType);
                    continue;
                }
                newFoundClasses.put(classType, newClass);
            }

            if (isMissingClass) {
                LOGGER.error("Analyze class {} failed, missing some dependency class: ", javaClass.getClassName());
                for (ClassType missingClass : missingClasses) {
                    LOGGER.error("Missing: {}", missingClass.getFullyQualifiedName());
                }
                continue;
            }

            for (Map.Entry<ClassType, JavaClass> entry : newFoundClasses.entrySet()) {
                allClasses.put(entry.getKey(), entry.getValue());
                classQueue.add(entry.getValue());
                LOGGER.info("Found new dependency class: {}", entry.getKey().getFullyQualifiedName());
            }
        }
        this.addJsonConstructorFlag();
        return allClasses.values();
    }

    private static class DependencyStmtVisitor extends AbstractStmtVisitor {
        private final DependencyValueVisitor valueVisitor;

        public DependencyStmtVisitor() {
            this.valueVisitor = new DependencyValueVisitor();
        }

        /**
         * Get Scan result
         *
         * @return classes
         */
        public Set<ClassType> getClasses() {
            return valueVisitor.getClasses();
        }

        /**
         * clear valueVisitor
         */
        public void clear() {
            valueVisitor.clear();
        }

        @Override
        public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
            if (stmt.getInvokeExpr().isPresent()) {
                stmt.getInvokeExpr().get().accept(valueVisitor);
            }
        }

        @Override
        public void caseAssignStmt(@Nonnull JAssignStmt stmt) {
            stmt.getLeftOp().accept(valueVisitor);
            stmt.getRightOp().accept(valueVisitor);
        }
    }

    private static class DependencyValueVisitor extends AbstractValueVisitor {
        private final Set<ClassType> classes;

        DependencyValueVisitor() {
            this.classes = new HashSet<>();
        }

        Set<ClassType> getClasses() {
            return classes;
        }

        void clear() {
            this.classes.clear();
        }

        @Override
        public void caseStaticInvokeExpr(@Nonnull JStaticInvokeExpr expr) {
            if (expr.getType() instanceof ClassType
                    && TranslatorContext.getIgnoredClasses().contains(
                            ((ClassType) expr.getType()).getFullyQualifiedName())) {
                return;
            }
            for (Immediate immediate : expr.getArgs()) {
                if (immediate.getType() instanceof ClassType
                        && TranslatorContext.getIgnoredClasses().contains(
                                ((ClassType) immediate.getType()).getFullyQualifiedName())) {
                    return;
                }
            }
            classes.add(expr.getMethodSignature().getDeclClassType());
        }

        @Override
        public void caseStaticFieldRef(@Nonnull JStaticFieldRef ref) {
            if (ref.getType() instanceof ClassType && !TranslatorContext.getIgnoredClasses()
                    .contains(((ClassType) ref.getType()).getFullyQualifiedName())) {
                classes.add(ref.getFieldSignature().getDeclClassType());
            }
        }

        @Override
        public void caseDynamicInvokeExpr(@Nonnull JDynamicInvokeExpr expr) {
            List<Immediate> bootstrapArgs = expr.getBootstrapArgs();
            MethodHandle invokeMethod;
            if (bootstrapArgs.get(1) instanceof MethodHandle) {
                invokeMethod = (MethodHandle) bootstrapArgs.get(1);
            } else {
                throw new TranslatorException("not supported dynamic invoke stmts");
            }

            if (bootstrapArgs.size() < 3 || invokeMethod.isFieldRef()) {
                throw new TranslatorException("not supported dynamic invoke stmts");
            }

            if (MethodHandle.Kind.REF_INVOKE_STATIC.equals(invokeMethod.getKind())) {
                classes.add(invokeMethod.getReferenceSignature().getDeclClassType());
            }
        }
    }
}
