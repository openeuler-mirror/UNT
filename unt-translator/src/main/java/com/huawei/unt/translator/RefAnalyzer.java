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

public class RefAnalyzer {
    private static  Map<String, Integer> refMap = TranslatorContext.LIB_INTERFACE_REF;
    private static  Map<String, Set<String>>  subclassMap = TranslatorContext.SUBCLASS_MAP;
    private static Set<JavaSootMethod> abstractMethods = new HashSet<>();

    public static void analyse(Collection<JavaClass> javaClasses) {
        for (JavaClass javaClass : javaClasses) {
            for (JavaSootMethod method : javaClass.getMethods()) {
                analyse(method);
            }
        }
        for (JavaSootMethod method : abstractMethods) {
            analyseAbstractMethod(method);
        }
    }

    private static void analyse(JavaSootMethod method) {
        String signature = method.getSignature().toString();
        Type returnType = method.getReturnType();
        System.out.println(method.getSignature().toString());
        if (! method.hasBody()) {
            abstractMethods.add(method);
        } else if (returnType instanceof VoidType
                || returnType instanceof PrimitiveType
                || isSimpleGet(method.getBody().getStmts())
        || TranslatorContext.IGNORED_METHODS.contains(method.getSignature().toString())) {
            refMap.put(signature, 0);
        } else {
            refMap.put(signature, 1);
        }
    }

    private static LinkedList<String> subclassQueue;
    private static Set<String> searched;
    private static void analyseAbstractMethod(JavaSootMethod method) {
        MethodSignature methodSignature = method.getSignature();
        String interfaceName = methodSignature.getDeclClassType().getFullyQualifiedName();
        String methodSignatureStr = methodSignature.toString();
        if (subclassMap.containsKey(interfaceName)) {
            subclassQueue = new LinkedList<>();
            searched = new HashSet<>();
            subclassQueue.addAll(subclassMap.get(interfaceName));
            while (! subclassQueue.isEmpty()) {
                String subclass = subclassQueue.poll();
                if (! searched.contains(subclass)) {
                    String tmpSignature = methodSignatureStr.replace(interfaceName, subclass);
                    if (refMap.containsKey(tmpSignature)) {
                        refMap.put(methodSignature.toString(), refMap.get(tmpSignature));
                        break;
                    }
                    if (subclassMap.containsKey(subclass)) {
                        subclassQueue.addAll(subclassMap.get(subclass));
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
