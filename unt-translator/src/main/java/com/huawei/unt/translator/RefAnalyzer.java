/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import com.huawei.unt.model.JavaClass;

import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;
import sootup.java.core.JavaSootMethod;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Use for analyze ref use for memory free
 *
 * @since 2025-05-19
 */
public class RefAnalyzer {
    private static final Map<String, Integer> REF_MAP = TranslatorContext.getLibInterfaceRef();
    private static final Map<String, Set<String>> SUBCLASS_MAP = TranslatorContext.getSubclassMap();
    private static final Set<JavaSootMethod> ABSTRACT_METHODS = new HashSet<>();

    /**
     * Analyze java classes
     *
     * @param javaClasses javaClasses
     */
    public static void analyse(Collection<JavaClass> javaClasses) {
        for (JavaClass javaClass : javaClasses) {
            for (JavaSootMethod method : javaClass.getMethods()) {
                analyse(method);
            }
        }
        for (JavaSootMethod method : ABSTRACT_METHODS) {
            analyseAbstractMethod(method);
        }
    }

    private static void analyse(JavaSootMethod method) {
        String signature = method.getSignature().toString();
        Type returnType = method.getReturnType();
        if (! method.hasBody()) {
            ABSTRACT_METHODS.add(method);
        } else if (returnType instanceof VoidType
                || returnType instanceof PrimitiveType
                || isSimpleGet(method.getBody().getStmts())
        || TranslatorContext.getIgnoredMethods().contains(method.getSignature().toString())) {
            REF_MAP.put(signature, 0);
        } else {
            REF_MAP.put(signature, 1);
        }
    }

    private static void analyseAbstractMethod(JavaSootMethod method) {
        MethodSignature methodSignature = method.getSignature();
        String interfaceName = methodSignature.getDeclClassType().getFullyQualifiedName();
        String methodSignatureStr = methodSignature.toString();
        if (SUBCLASS_MAP.containsKey(interfaceName)) {
            LinkedList<String> subclassQueue = new LinkedList<>();
            Set<String> searched = new HashSet<>();
            subclassQueue.addAll(SUBCLASS_MAP.get(interfaceName));
            while (! subclassQueue.isEmpty()) {
                String subclass = subclassQueue.poll();
                if (! searched.contains(subclass)) {
                    String tmpSignature = methodSignatureStr.replace(interfaceName, subclass);
                    if (REF_MAP.containsKey(tmpSignature)) {
                        REF_MAP.put(methodSignatureStr, REF_MAP.get(tmpSignature));
                        break;
                    }
                    if (SUBCLASS_MAP.containsKey(subclass)) {
                        subclassQueue.addAll(SUBCLASS_MAP.get(subclass));
                    }
                    searched.add(subclass);
                }
            }
        }
    }

    private static boolean isSimpleGet(List<Stmt> stmts) {
        if (stmts.size() == 3) {
            Stmt stmt0 = stmts.get(0);
            if (stmt0 instanceof JIdentityStmt && ((JIdentityStmt) stmt0).getRightOp() instanceof JThisRef) {
                Local thisLocal = ((JIdentityStmt) stmt0).getLeftOp();
                Stmt stmt1 = stmts.get(1);
                if (stmt1 instanceof JAssignStmt) {
                    LValue leftOp = ((JAssignStmt) stmt1).getLeftOp();
                    Value value = ((JAssignStmt) stmt1).getRightOp();
                    if (value instanceof JInstanceFieldRef && ((JInstanceFieldRef) value).getBase().equals(thisLocal)) {
                        Stmt stmt2 = stmts.get(2);
                        return stmt2 instanceof JReturnStmt && ((JReturnStmt) stmt2).getOp().equals(leftOp);
                    }
                }
            }
        }
        return isNullBody(stmts);
    }

    private static boolean isNullBody(List<Stmt> stmts) {
        Stmt stmt = stmts.get(stmts.size() - 1);
        return stmt instanceof JReturnStmt && ((JReturnStmt) stmt).getOp() instanceof NullConstant;
    }
}
