/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedLinesStmt;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test NewRefOptimizer optimizer
 *
 * @since 2025-06-10
 */
class NewRefOptimizerTest extends BaseTest {
    private static final NewRefOptimizer NEW_REF_OPTIMIZER = new NewRefOptimizer();
    private static JarHandler jarHandler;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);

        JavaView javaView = new JavaView(inputLocation);

        jarHandler = new JarHandler(javaView);
    }

    @Test
    void testBasicNewRef() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestNewRef", "testBasicNewRef", "void", ImmutableList.of());
        assertTrue(method.isPresent());
        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);

        assertTrue(NEW_REF_OPTIMIZER.fetch(methodContext));

        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            Map<Integer, Set<Local>> beforeLocals = new HashMap<>();
            Set<Local> locals = new HashSet<>();
            beforeLocals.put(i, locals);
            methodContext.setBeforeLocals(beforeLocals);
        }

        NEW_REF_OPTIMIZER.optimize(methodContext);
        Stmt newStmt = methodContext.getStmts().get(1);
        Stmt initStmt = methodContext.getStmts().get(2);
        assertTrue(newStmt instanceof OptimizedLinesStmt
                && ((OptimizedLinesStmt) newStmt).getOptimizedCodes().isEmpty());
        assertTrue(initStmt instanceof OptimizedLinesStmt
                && "r1 = new java_lang_String();".equals(((OptimizedLinesStmt) initStmt).getOptimizedCodes().get(0)));
    }
}