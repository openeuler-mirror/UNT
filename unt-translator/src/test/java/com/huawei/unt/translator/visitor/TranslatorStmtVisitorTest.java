/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator.visitor;

import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.BranchStmtLabeler;
import com.huawei.unt.type.NoneUDF;

import com.google.common.collect.ImmutableList;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.javabytecode.stmt.JEnterMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JExitMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.PrimitiveType;
import sootup.core.types.UnknownType;
import sootup.java.bytecode.frontend.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Test TranslatorStmtVisitor
 *
 * @since 2025-05-19
 */
public class TranslatorStmtVisitorTest extends BaseTest {
    private static final String NEW_LINE = System.lineSeparator();

    private static JarHandler jarHandler;
    private static MethodContext methodContext;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        jarHandler = new JarHandler(javaView);
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestBranch", "testIf", "void", ImmutableList.of("int"));
        methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        new BranchStmtLabeler().optimize(methodContext);
    }

    @Test
    public void testInvokeStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JSpecialInvokeExpr specialInvokeExpr = new JSpecialInvokeExpr(
                new Local("id", PrimitiveType.IntType.getInstance()),
                new MethodSignature(
                        JavaIdentifierFactory.getInstance().getClassType("testClass"),
                        "testMethod",
                        ImmutableList.of(PrimitiveType.IntType.getInstance()),
                        PrimitiveType.IntType.getInstance()),
                ImmutableList.of(new Local("param", PrimitiveType.IntType.getInstance())));
        JInvokeStmt invokeStmt = new JInvokeStmt(specialInvokeExpr, StmtPositionInfo.getNoStmtPositionInfo());

        invokeStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "id->testMethod(param);" + NEW_LINE);
    }

    @Test
    public void testIdentityStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JIdentityStmt identityStmt = new JIdentityStmt(
                new Local("l", PrimitiveType.IntType.getInstance()),
                new JThisRef(methodContext.getJavaMethod().getDeclaringClassType()),
                StmtPositionInfo.getNoStmtPositionInfo());

        identityStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "");
    }

    @Test
    public void testAssignStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JAssignStmt assignStmt = new JAssignStmt(new Local("left", PrimitiveType.IntType.getInstance()),
                IntConstant.getInstance(10), StmtPositionInfo.getNoStmtPositionInfo());

        assignStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "left = 10;" + NEW_LINE);
    }

    @Test
    public void testReturnStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JReturnStmt returnStmt = new JReturnStmt(new Local("r", PrimitiveType.IntType.getInstance()),
                StmtPositionInfo.getNoStmtPositionInfo());

        returnStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), NEW_LINE + "return r;" + NEW_LINE);
    }

    @Test
    public void testReturnVoidStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JReturnVoidStmt returnVoidStmt = new JReturnVoidStmt(StmtPositionInfo.getNoStmtPositionInfo());

        returnVoidStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), NEW_LINE + "return;" + NEW_LINE);
    }

    @Test
    public void testGotoStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JGotoStmt gotoStmt = null;

        for (Stmt stmt : methodContext.getStmts()) {
            if (stmt instanceof JGotoStmt) {
                gotoStmt = (JGotoStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(gotoStmt);

        gotoStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "goto label1;" + NEW_LINE);
    }

    @Test
    public void caseIfStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JIfStmt ifStmt = null;

        for (Stmt stmt : methodContext.getStmts()) {
            if (stmt instanceof JIfStmt) {
                ifStmt = (JIfStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(ifStmt);
        ifStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "if ( i0 <= 10 ) {" + NEW_LINE
                + "    goto label0;" + NEW_LINE
                + "}" + NEW_LINE);
    }

    @Test
    public void enterMonitorTest() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JEnterMonitorStmt enterMonitorStmt = new JEnterMonitorStmt(
                new Local("local", UnknownType.getInstance()), StmtPositionInfo.getNoStmtPositionInfo());

        enterMonitorStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "local->mutex.lock();" + NEW_LINE);
    }

    @Test
    public void exitMonitorTest() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);
        JExitMonitorStmt exitMonitorStmt = new JExitMonitorStmt(
                new Local("local", UnknownType.getInstance()), StmtPositionInfo.getNoStmtPositionInfo());

        exitMonitorStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "local->mutex.unlock();" + NEW_LINE);
    }

    @Test
    public void switchTest() {
        Optional<JavaSootMethod> method = jarHandler.tryGetMethod(
                "TestBranch", "testSwitch", "void", ImmutableList.of("int"));

        MethodContext switchMethodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        new BranchStmtLabeler().optimize(switchMethodContext);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(switchMethodContext, 0, false);

        JSwitchStmt switchStmt = null;
        for (Stmt stmt : switchMethodContext.getStmts()) {
            if (stmt instanceof JSwitchStmt) {
                switchStmt = (JSwitchStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(switchStmt);
        switchStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "switch (i0) {" + NEW_LINE
                + "    case 0:" + NEW_LINE
                + "        goto label0;" + NEW_LINE
                + "    case 1:" + NEW_LINE
                + "        goto label1;" + NEW_LINE
                + "    case 2:" + NEW_LINE
                + "        goto label2;" + NEW_LINE
                + "    case 3:" + NEW_LINE
                + "        goto label3;" + NEW_LINE
                + "    default:" + NEW_LINE
                + "        goto label4;" + NEW_LINE
                + "}" + NEW_LINE);
    }
}
