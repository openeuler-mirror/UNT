package com.huawei.unt.optimizer;

import com.huawei.unt.translator.TranslatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huawei.unt.model.MethodContext;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JGotoStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class MemoryReleaseOptimizer implements Optimizer {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryReleaseOptimizer.class);
    public final static Integer BEFORE = 0;
    public final static Integer AFTER = 1;

    private final Map<String, Integer> refMap;
    private List<Stmt> stmts;
    private List<Stmt> stmtsToBeTranslated;
    private Map<Local, Integer> locals;
    private Map<Local, List<Integer>> localAssign;
    private Map<Local, Local> localMap;
    private Map<Integer, Map<LValue, Integer>> unknownFree;
    private Map<Integer, Map<LValue, Integer>> circleFree;
    private Map<Integer, Map<LValue, Integer>> getRef;
    private Map<Integer, Set<LValue>> makeNullSet;
    private Set<Integer> gotoFreeStmts;
    private Map<Integer, Immediate> retStmts;
    private Map<Integer, Local> clearStmts;
    private Set<Integer> reassignStmts;
    private Set<Local> reassignLocals;
    private Set<Integer> ignoredReturnValues;

    private int finalReturn;
    private boolean needRet;
    private boolean needFree;
    private String methodSignature;
    private boolean isStaticInit;
    private boolean isInit;
    private Map<Integer, Integer> loops;

    public MemoryReleaseOptimizer(Map<String, Integer> refMap) {
        this.refMap = refMap;
    }

    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        isStaticInit = methodContext.isStaticInit();
        isInit = methodContext.isInit();
        stmts = methodContext.getJavaMethod().getBody().getStmts();
        methodSignature = methodContext.getJavaMethod().getSignature().toString();
        stmtsToBeTranslated = methodContext.getStmts();
        locals = new HashMap<>();
        localAssign = new HashMap<>();
        localMap = new HashMap<>();
        unknownFree = new HashMap<>();
        circleFree = new HashMap<>();
        getRef = new HashMap<>();
        makeNullSet = new HashMap<>();
        gotoFreeStmts = new HashSet<>();
        retStmts = new HashMap<>();
        clearStmts = new HashMap<>();
        reassignStmts = new HashSet<>();
        reassignLocals = new HashSet<>();
        ignoredReturnValues = new HashSet<>();
        finalReturn = -1;
        needRet = false;
        needFree = false;
        Map<Integer, LValue> iterFree = new HashMap<>();

        findLoops(methodContext);

        for (int i = 0; i < stmts.size(); i++) {

            if (methodContext.isRemoved(i)) {
                continue;
            }

            Stmt stmt = stmts.get(i);

            if (loops.containsKey(i)) {
                i = loopHandle(i, methodContext);
            } else if (stmt instanceof JIdentityStmt) {
                identityStmtHandle(i);
            } else if (stmt instanceof JAssignStmt) {
                assignStmtHandle(i);
            } else if (stmt instanceof JReturnStmt) {
                returnStmtHandle(i);
            } else if (stmt instanceof JReturnVoidStmt) {
                returnVoidStmtHandle(i);
            } else if (stmt instanceof JInvokeStmt) {
                invokeStmtHandle(i);
            }
        }

        if (needRet) {
            methodContext.setRet();
            retStmts.put(finalReturn, ((JReturnStmt) stmts.get(finalReturn)).getOp());
            stmtsToBeTranslated.set(finalReturn, Optimizers.getEmptyOptimizedStmt(stmts.get(finalReturn)));
        }
        freeHandle(finalReturn, locals.keySet(), BEFORE, false);
        if (needFree) {
            methodContext.setFreeLabel(finalReturn);
        }

        methodContext.setRetStmts(retStmts);
        methodContext.setGotoFreeStmts(gotoFreeStmts);
        methodContext.setUnknownFree(unknownFree);
        methodContext.setCircleFree(circleFree);
        methodContext.setIterFree(iterFree);
        methodContext.setGetRef(getRef);
        methodContext.setMakeNull(makeNullSet);
        methodContext.setClearStmts(clearStmts);
        methodContext.setReassignStmts(reassignStmts);
        methodContext.setReassignLocals(reassignLocals);
        methodContext.setIgnoredReturnValues(ignoredReturnValues);
    }

    // label stmt, reachable
    private Map<Integer, Set<Integer>> labelGraph;
    // back stmt, belonged label block
    private Map<Integer, Integer> backupPoints;

    private void findLoops(MethodContext methodContext) {
        loops = new HashMap<>();
        getLabelGraph(methodContext);
        getLoops(methodContext);

        for (int head : loops.keySet()) {
            int tail = loops.get(head);
            boolean hasBreak = false;
            loops.put(head, tail + 1);
            for (int i = head; i <= tail; i++) {
                if (methodContext.isRemoved(i)) continue;
                Stmt stmt = stmts.get(i);
                if (stmt.branches()) {
                    for (int target : methodContext.getBranchTargets(stmt)) {
                        if(target > tail) {
                            loops.put(head, target);
                            hasBreak = true;
                            break;
                        }
                    }
                }
                if (hasBreak) break;
            }
        }
    }

    private void getLoops(MethodContext methodContext) {
        for (int back : backupPoints.keySet()) {
            int from = backupPoints.get(back);
            Stmt stmt = stmts.get(back);
            if (stmt.branches()) {
                for (Integer target : methodContext.getBranchTargets(stmt)) {
                    if (labelGraph.get(target).contains(from)) {
                        int lastGoto = loops.getOrDefault(target, -1);
                        lastGoto = Math.max(back, lastGoto);
                        loops.put(target, lastGoto);
                    }
                }
            }
        }
    }

    private void getLabelGraph(MethodContext methodContext) {
        labelGraph = new HashMap<>();
        backupPoints = new HashMap<>();

        for (int i = 0; i < stmts.size(); i++) {
            if (methodContext.containsLabel(i)) {
                i = findBlockTarget(i, methodContext);
            }
        }

        updateLabelGraph();
    }

    private void updateLabelGraph() {
        Set<Integer> visited = new HashSet<>();
        for (int label : labelGraph.keySet()) {
            findReachableLabel(label, visited);
        }
    }

    private void findReachableLabel(int label, Set<Integer> visited) {
        if (!visited.contains(label)) {
            Set<Integer> reachable = new HashSet<>(labelGraph.get(label));
            for (int target : labelGraph.get(label)) {
                findReachableLabel(target, visited);
                reachable.addAll(labelGraph.get(target));
            }
            reachable.add(label);
            labelGraph.put(label, reachable);
            visited.add(label);
        }
    }

    private int findBlockTarget(int i, MethodContext methodContext) {
        int j = i;
        Set<Integer> targets = new HashSet<>();
        while (j < stmts.size()) {
            if (methodContext.isRemoved(j)) {
                j++;
                continue;
            }
            Stmt stmt = stmts.get(j);
            if(stmt.branches()) {
                for (Integer branchTarget : methodContext.getBranchTargets(stmt)) {
                    if(branchTarget > i) {
                        targets.add(branchTarget);
                    } else {
                        backupPoints.put(j, i);
                    }
                }
            }
            if(stmt instanceof JGotoStmt || stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt) {
                break;
            }
            j++;
            if (methodContext.containsLabel(j)) {
                targets.add(j);
                j--;
                break;
            }
        }
        labelGraph.put(i, targets);
        return j;
    }

    // todo: udf inputs mark
    private void identityStmtHandle(int i) {
        Stmt stmt = stmts.get(i);
        Local local = ((JIdentityStmt) stmt).getLeftOp();
        if (!(local.getType() instanceof PrimitiveType)) {
            locals.put(local, 0);
            List<Integer> locations = new ArrayList<>();
            locations.add(i);
            localAssign.put(local, locations);
        }
    }

    private void assignStmtHandle(int i) {
        Stmt stmt = stmts.get(i);
        LValue leftOp = ((JAssignStmt) stmt).getLeftOp();

        Value value = ((JAssignStmt) stmt).getRightOp();
        int ref = getRef(value);
        if (!(leftOp.getType() instanceof PrimitiveType)) {
            if (leftOp instanceof Local) {
                localAssignStmtHandle(i, (Local) leftOp, value, ref);
            } else if (leftOp instanceof JFieldRef){
                fieldRefAssignStmtHandle(i, leftOp, value, ref);
            } else if (leftOp instanceof JArrayRef) {
                arrRefAssignStmtHandle(i, leftOp, value, ref);
            }
        }
    }

    private void localAssignStmtHandle(int i, Local local, Value value, int ref) {
        if (localAssign.containsKey(local)) {
            reassignHelper(i, ref);
        } else {
            locals.put(local, ref);
            List<Integer> assignLocation = new ArrayList<>();
            assignLocation.add(i);
            localAssign.put(local, assignLocation);
        }

        updateLocalMap(local, value, ref);
    }

    private void fieldRefAssignStmtHandle(int i, LValue leftOp, Value value, int ref) {

        if (!isStaticInit && !isInit) {
            Map<LValue, Integer> vars = unknownFree.getOrDefault(i, new HashMap<>());
            vars.put(leftOp, BEFORE);
            unknownFree.put(i, vars);
        }
        if (ref == 0) {
            if (value instanceof Local && locals.containsKey(value) && locals.get(value) == 1) {
                locals.put((Local) value, 0);
            } else if (value instanceof JCastExpr) {
                Immediate op = ((JCastExpr) value).getOp();
                if (op instanceof Local && locals.containsKey(op) && locals.get(op) == 1) {
                    locals.put((Local) op, 0);
                }
            } else if (!((JFieldRef) leftOp).getFieldSignature().getName().equals("this$0")) {
                Map<LValue, Integer> vars = getRef.getOrDefault(i, new HashMap<>());
                vars.put(leftOp, AFTER);
                getRef.put(i, vars);
            }
        }
    }

    // deal with arr[0] = new ...  arr->append
    private void arrRefAssignStmtHandle(int i, LValue leftOp, Value value, int ref) {
        if (ref == 1 && leftOp instanceof JArrayRef) {
            Map<LValue, Integer> vars = unknownFree.getOrDefault(i, new HashMap<>());
            vars.put(leftOp, AFTER);
            unknownFree.put(i, vars);
        }
    }

    private void updateLocalMap(Local local, Value value, int ref) {
        if (ref == 1) {
            localMap.put(local, local);
        } else {
            if (value instanceof Local && localMap.containsKey(value)) {
                localMap.put(local, localMap.get(value));
            } else if (value instanceof JCastExpr) {
                Immediate op = ((JCastExpr) value).getOp();
                if (op instanceof Local && localMap.containsKey(op)) {
                    localMap.put(local, localMap.get(op));
                }
            }
        }
    }

    private void reassignHelper(int i, int ref) {
        Stmt stmt = stmts.get(i);
        Local leftOp = (Local) ((JAssignStmt) stmt).getLeftOp();
        List<Integer> locations = localAssign.get(leftOp);
        if (locals.get(leftOp) != 0 || ref != 0) {
            reassignStmts.add(i);
            reassignLocals.add(leftOp);
            if (locals.get(leftOp) == 0) {
                int firstAssignLocation = locations.get(0);
                Map<LValue, Integer> getRefs = getRef.getOrDefault(firstAssignLocation, new HashMap<>());
                getRefs.put(leftOp, AFTER);
                getRef.put(firstAssignLocation, getRefs);
                for (int j = 1; j < locations.size(); j++) {
                    int location = locations.get(j);
                    getRefs = getRef.getOrDefault(location, new HashMap<>());
                    getRefs.put(leftOp, AFTER);
                    getRef.put(location, getRefs);
                    reassignStmts.add(j);
                }
                locals.put(leftOp, 1);
            } else if (ref == 0) {
                Map<LValue, Integer> getRefs = getRef.getOrDefault(i, new HashMap<>());
                getRefs.put(leftOp, AFTER);
                getRef.put(i, getRefs);
            }
        }
        locations.add(i);
    }

    private int loopHandle(int i, MethodContext methodContext) {
        // loop start from i, break to loops.get(i)
        List<Integer> continueStmts = new ArrayList<>();
        Set<Local> blockLocals = new HashSet<>();

        int breakTarget = loops.get(i);
        for (int j = i; j < breakTarget; j++) {
            // skip removed stmts
            if (methodContext.isRemoved(j)) {
                continue;
            }

            if (j > i && loops.containsKey(j)) {
                j = loopHandle(j, methodContext);
                continue;
            }
            Stmt stmt = stmts.get(j);
            if (stmt.branches()) {
                // find continue
                List<Integer> branchTargets = methodContext.getBranchTargets(stmt);
                if (branchTargets.get(0) == i) {
                    continueStmts.add(j);
                }
            } else if (stmt instanceof JAssignStmt) {
                loopAssignStmtHandle(j, blockLocals);
            } else if (stmt instanceof JInvokeStmt) {
                invokeStmtHandle(j);
            } else if (stmt instanceof JReturnStmt) {
                returnStmtHandle(j);
            } else if (stmt instanceof JReturnVoidStmt) {
                returnVoidStmtHandle(j);
            }
        }

        for (Integer end : continueStmts) {
            freeHandle(end, blockLocals, BEFORE, true);
        }
        freeHandle(breakTarget, blockLocals, BEFORE, true);

        for (Local local : blockLocals) {
            locals.put(local, 0);
        }

        return breakTarget - 1;
    }

    private void loopAssignStmtHandle(int j, Set<Local> blockLocals) {
        Stmt stmt = stmts.get(j);
        LValue leftOp = ((JAssignStmt) stmt).getLeftOp();

        Value value = ((JAssignStmt) stmt).getRightOp();
        int ref = getRef(value);
        if (! (leftOp.getType() instanceof PrimitiveType)) {
            if (leftOp instanceof Local) {
                if (localAssign.containsKey(leftOp)) {
                    reassignHelper(j, ref);
                } else {
                    blockLocals.add((Local) leftOp);
                    locals.put((Local) leftOp, ref);
                    List<Integer> assignLocation = new ArrayList<>();
                    assignLocation.add(j);
                    localAssign.put((Local) leftOp, assignLocation);
                }
            } else if (leftOp instanceof JFieldRef){
                fieldRefAssignStmtHandle(j, leftOp, value, ref);
            } else if (leftOp instanceof JArrayRef){
                arrRefAssignStmtHandle(j, leftOp, value, ref);
            }
        }
    }

    private void invokeStmtHandle(int i) {
        Stmt stmt = stmts.get(i);
        if (stmt instanceof JInvokeStmt) {
            Optional<AbstractInvokeExpr> expr = ((JInvokeStmt) stmt).getInvokeExpr();
//            if (expr.isPresent() && returnObj(expr.get()) && getRef(expr.get()) == 1) {
            if (expr.isPresent() && getRef(expr.get()) == 1) {
                ignoredReturnValues.add(i);
            }
        }
    }

//    private boolean returnObj(AbstractInvokeExpr expr) {
//        Type type = expr.getType();
//        return ! (type instanceof PrimitiveType || type instanceof VoidType);
//    }

    private void returnStmtHandle(int i) {
        if (finalReturn != -1) {
            needRet = true;
            retStmts.put(finalReturn, ((JReturnStmt) stmts.get(finalReturn)).getOp());
            needFree = true;
            gotoFree(finalReturn);
        }
        finalReturn = i;
        if (refMap.get(methodSignature) == 1) {
            returnRefHandle(i);
        }
    }

    private void returnVoidStmtHandle(int i) {
        if (finalReturn != -1) {
            needFree = true;
            gotoFree(finalReturn);
        }
        finalReturn = i;
    }



    private void gotoFree(int i) {
        gotoFreeStmts.add(i);
        stmtsToBeTranslated.set(i, Optimizers.getEmptyOptimizedStmt(stmts.get(i)));
    }

    private void returnRefHandle(int i) {
        Stmt returnStmt = stmts.get(i);
        Immediate returnOp = ((JReturnStmt) returnStmt).getOp();
        if (returnOp instanceof Local && locals.containsKey(returnOp)) {
            if (locals.get(returnOp) == 0) {
                if (localMap.containsKey(returnOp)) {
                    locals.put(localMap.get(returnOp), 0);
                } else {
                    Map<LValue, Integer> vars = getRef.getOrDefault(i, new HashMap<>());
                    vars.put((Local) returnOp, BEFORE);
                    getRef.put(i, vars);
                }
            } else {
                locals.put((Local) returnOp, 0);
            }
        }
    }

    private void freeHandle(int i, Set<Local> blockLocals, int pos, boolean inCircle) {
        Map<Integer, Map<LValue, Integer>> freeMap;

        if (inCircle) {
            freeMap = circleFree;
        } else {
            freeMap = unknownFree;
        }
        Map<LValue, Integer> vars = freeMap.getOrDefault(i, new HashMap<>());

        for (Local local : blockLocals) {
            if (locals.get(local) == 1) {
                vars.put(local, pos);
            }
        }

        freeMap.put(i, vars);
    }

    private int getRef(Value value) {
        // todo: check new
        if (isNewExpr(value) || value instanceof StringConstant || isToString(value)) {
            return 1;
        } else if (value instanceof AbstractInvokeExpr) {
            MethodSignature methodSignature = ((AbstractInvokeExpr) value).getMethodSignature();
            ClassType classType = methodSignature.getDeclClassType();
            if (refMap.containsKey(methodSignature.toString())) {
                return refMap.get(methodSignature.toString());
            } else {
                int refTmp = -1;
                if (value instanceof JVirtualInvokeExpr || value instanceof JInterfaceInvokeExpr) {
                    if (TranslatorContext.SUPERCLASS_MAP.containsKey(classType.getFullyQualifiedName())) {
                        refTmp = searchSuperClass(methodSignature);
                        superClassQueue.clear();
                        searched.clear();
                    }
                }
                if (refTmp != -1) {
                    return refTmp;
                }
            }
            putMissingInterfaces(methodSignature);
            LOGGER.warn(String.format(
                    "the ref of method %s not found in refMap, use default 0 as ref count", methodSignature));
        }
        return 0;
    }

    private void putMissingInterfaces(MethodSignature methodSignature) {
        String className = methodSignature.getDeclClassType().getFullyQualifiedName();
        Set<String> missingMethods = TranslatorContext.MISSING_INTERFACES.getOrDefault(className, new HashSet<>());
        missingMethods.add(methodSignature.getSubSignature().toString());
        TranslatorContext.MISSING_INTERFACES.put(methodSignature.getDeclClassType().getFullyQualifiedName(), missingMethods);
    }

    private final LinkedList<String> superClassQueue = new LinkedList<>();
    private final Set<String> searched = new HashSet<>();

    private int searchSuperClass(MethodSignature methodSignature) {
        String className = methodSignature.getDeclClassType().getFullyQualifiedName();
        superClassQueue.addAll(TranslatorContext.SUPERCLASS_MAP.get(className));
        while (! superClassQueue.isEmpty()) {
            String superClass = superClassQueue.removeFirst();
            if (! searched.contains(superClass)) {
                String methodSignatureStr = methodSignature.toString();
                methodSignatureStr = methodSignatureStr.replace(className, superClass);
                if (refMap.containsKey(methodSignatureStr)) {
                    return refMap.get(methodSignatureStr);
                }
                if (TranslatorContext.SUPERCLASS_MAP.containsKey(superClass)) {
                    superClassQueue.addAll(
                            TranslatorContext.SUPERCLASS_MAP.get(superClass));
                }
                searched.add(superClass);
            }
        }
        return -1;
    }

    private boolean isNewExpr(Value value) {
        return value instanceof JNewExpr || value instanceof JNewArrayExpr || value instanceof JNewMultiArrayExpr;
    }

    private boolean isToString(Value value) {
        if (value instanceof AbstractInstanceInvokeExpr) {
            return ((AbstractInstanceInvokeExpr) value).getMethodSignature().getName().equals("toString");
        }
        return false;
    }
}
