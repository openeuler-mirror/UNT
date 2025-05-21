package com.huawei.unt.translator.visitor;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.BaseTest;
import com.huawei.unt.loader.JarHandler;
import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.BranchStmtLabeler;
import com.huawei.unt.type.NoneUDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class TranslatorStmtVisitorTest extends BaseTest {
    private static JarHandler JAR_HANDLER;
    private static MethodContext METHOD_CONTEXT;

    @BeforeAll
    public static void init() {
        Path binaryPath = Paths.get("src/test/resources/optimizer/binary");
        AnalysisInputLocation inputLocation =
                PathBasedAnalysisInputLocation.create(binaryPath, SourceType.Application, INTERCEPTORS);
        JavaView javaView = new JavaView(inputLocation);
        JAR_HANDLER = new JarHandler(javaView);
        Optional<JavaSootMethod> method = JAR_HANDLER.tryGetMethod("TestBranch", "testIf", "void", ImmutableList.of("int"));
        METHOD_CONTEXT = new MethodContext(method.get(), NoneUDF.INSTANCE);
        new BranchStmtLabeler().optimize(METHOD_CONTEXT);
    }

    @Test
    public void testInvokeStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

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

        Assertions.assertEquals(stmtVisitor.toCode(), "id->testMethod(param);\n");
    }

    @Test
    public void testIdentityStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JIdentityStmt identityStmt = new JIdentityStmt(
                new Local("l", PrimitiveType.IntType.getInstance()),
                new JThisRef(METHOD_CONTEXT.getJavaMethod().getDeclaringClassType()),
                StmtPositionInfo.getNoStmtPositionInfo());

        identityStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "");
    }

    @Test
    public void testAssignStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JAssignStmt assignStmt = new JAssignStmt(new Local("left", PrimitiveType.IntType.getInstance()),
                IntConstant.getInstance(10), StmtPositionInfo.getNoStmtPositionInfo());

        assignStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "left = 10;\n");
    }

    @Test
    public void testReturnStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JReturnStmt returnStmt = new JReturnStmt(new Local("r", PrimitiveType.IntType.getInstance()),
                StmtPositionInfo.getNoStmtPositionInfo());

        returnStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "\nreturn r;\n");
    }

    @Test
    public void testReturnVoidStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JReturnVoidStmt returnVoidStmt = new JReturnVoidStmt(StmtPositionInfo.getNoStmtPositionInfo());

        returnVoidStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "\nreturn;\n");
    }

    @Test
    public void testGotoStmt() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JGotoStmt gotoStmt = null;

        for (Stmt stmt : METHOD_CONTEXT.getStmts()) {
            if (stmt instanceof JGotoStmt) {
                gotoStmt = (JGotoStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(gotoStmt);

        gotoStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "goto label1;\n");
    }

    @Test
    public void caseIfStmt() {

        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JIfStmt ifStmt = null;

        for (Stmt stmt : METHOD_CONTEXT.getStmts()) {
            if (stmt instanceof JIfStmt) {
                ifStmt = (JIfStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(ifStmt);
        ifStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "if ( i0 <= 10 ) {\n" +
                "    goto label0;\n" +
                "}\n");
    }

    @Test
    public void enterMonitorTest() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);

        JEnterMonitorStmt enterMonitorStmt = new JEnterMonitorStmt(new Local("local", UnknownType.getInstance()), StmtPositionInfo.getNoStmtPositionInfo());

        enterMonitorStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "local->mutex.lock();\n");
    }

    @Test
    public void exitMonitorTest() {
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(METHOD_CONTEXT, 0, false);
        JExitMonitorStmt exitMonitorStmt = new JExitMonitorStmt(new Local("local", UnknownType.getInstance()), StmtPositionInfo.getNoStmtPositionInfo());

        exitMonitorStmt.accept(stmtVisitor);

        Assertions.assertEquals(stmtVisitor.toCode(), "local->mutex.unlock();\n");
    }

    @Test
    public void switchTest() {
        Optional<JavaSootMethod> method = JAR_HANDLER.tryGetMethod("TestBranch", "testSwitch", "void", ImmutableList.of("int"));

        MethodContext methodContext = new MethodContext(method.get(), NoneUDF.INSTANCE);
        new BranchStmtLabeler().optimize(methodContext);
        TranslatorStmtVisitor stmtVisitor = new TranslatorStmtVisitor(methodContext, 0, false);

        JSwitchStmt switchStmt = null;
        for (Stmt stmt : methodContext.getStmts()) {
            if (stmt instanceof JSwitchStmt) {
                switchStmt = (JSwitchStmt) stmt;
                break;
            }
        }

        Assertions.assertNotNull(switchStmt);
        switchStmt.accept(stmtVisitor);
        Assertions.assertEquals(stmtVisitor.toCode(), "switch (i0) {\n" +
                "    case 0:\n" +
                "        goto label0;\n" +
                "    case 1:\n" +
                "        goto label1;\n" +
                "    case 2:\n" +
                "        goto label2;\n" +
                "    case 3:\n" +
                "        goto label3;\n" +
                "    default:\n" +
                "        goto label4;\n" +
                "}\n");
    }
}
