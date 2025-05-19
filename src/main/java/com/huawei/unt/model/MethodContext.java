package com.huawei.unt.model;

import com.huawei.unt.dependency.DependencyAnalyzer;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.type.UDFType;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractStmtVisitor;
import sootup.core.jimple.visitor.AbstractValueVisitor;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootMethod;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodContext {
    private final JavaSootMethod javaMethod;

    private final UDFType type;
    private final Set<Local> locals;
    private final List<Stmt> stmts;

    private final Map<Integer, Local> params = new HashMap<>();
    //some locals are merged for some reason, e.g. StringBuilder
//    private final HashMap<Local,Local> mergedLocals = new HashMap<>();
    List<Integer> labels = new ArrayList<>();
    Map<Integer, List<Integer>> branches = new HashMap<>();
    private final Local thisLocal;
    private List<JavaTrap> traps;
    private boolean needRet = false;
    private int freeLabel = -1;
    private final boolean isStaticInit;
    private final boolean isInit;
    private final boolean isIgnore;

    private final HashSet<Integer> removedStmts = new HashSet<>();

//    private final Map<Integer, Map<LValue, Integer>> simpleFree = new HashMap<>();
    private final Map<Integer, Map<LValue, Integer>> unknownFree = new HashMap<>();
    private final Map<Integer, Map<LValue, Integer>> circleFree = new HashMap<>();
    private final Map<Integer, LValue> iterFree = new HashMap<>();
    private final Map<Integer, Map<LValue, Integer>> getRef = new HashMap<>();
    private final Set<Integer> reassignStmts = new HashSet<>();
    private final Set<Local> reassignLocals = new HashSet<>();
    private final Set<Integer> ignoredReturnValues = new HashSet<>();
    private final Map<Integer, Set<LValue>> makeNull = new HashMap<>();
    private final Set<Integer> gotoFreeStmts = new HashSet<>();
    private final Map<Integer, Immediate> retStmts = new HashMap<>();
//    private final List<Integer> nextLabels = new ArrayList<>();
//    private final Map<Integer, Integer> gotoNextStmts = new HashMap<>();
    private final Map<Integer, Local> clearStmts = new HashMap<>();
    private final Map<Integer, Set<Local>> beforeLocals = new HashMap<>();
    private final Map<Integer, Set<Local>> afterLocals = new HashMap<>();

    private final Set<ClassType> staticClasses = new HashSet<>();

    private static class StaticStmtVisitor extends AbstractStmtVisitor {
        private final StaticValueVisitor valueVisitor;

        public StaticStmtVisitor() {
            this.valueVisitor = new StaticValueVisitor();
        }

        public Set<ClassType> getClasses() {
            return valueVisitor.getClasses();
        }

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

    private static class StaticValueVisitor extends AbstractValueVisitor {
        private final Set<ClassType> classes;

        public StaticValueVisitor() {
            this.classes = new HashSet<>();
        }

        public Set<ClassType> getClasses() {
            return classes;
        }

        public void clear() {
            this.classes.clear();
        }

        @Override
        public void caseStaticInvokeExpr(@Nonnull JStaticInvokeExpr expr) {
//            if (expr.getType() instanceof ClassType &&
//                    TranslatorContext.IGNORED_CLASSES.contains(((ClassType) expr.getType()).getFullyQualifiedName())) {
//                return;
//            }
//            for (Immediate immediate : expr.getArgs()) {
//                if (immediate.getType() instanceof ClassType &&
//                        TranslatorContext.IGNORED_CLASSES.contains(
//                                ((ClassType) immediate.getType()).getFullyQualifiedName())) {
//                    return;
//                }
//            }
//            classes.add(expr.getMethodSignature().getDeclClassType());
        }

        @Override
        public void caseStaticFieldRef(@Nonnull JStaticFieldRef ref) {
            if (ref.getType() instanceof ClassType && !TranslatorContext.IGNORED_CLASSES
                    .contains(((ClassType) ref.getType()).getFullyQualifiedName())) {
                classes.add(ref.getFieldSignature().getDeclClassType());
            }
        }
    }


    public MethodContext(JavaSootMethod method, UDFType type) {

        StaticStmtVisitor stmtVisitor = new StaticStmtVisitor();

        for (Stmt stmt : method.getBody().getStmts()) {
            stmt.accept(stmtVisitor);
        }
        Set<ClassType> classes = stmtVisitor.getClasses();
        Set<ClassType> staticSet = classes.stream()
                .filter(c -> !TranslatorContext.CLASS_MAP.containsKey(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (method.getName().equals(TranslatorContext.STATIC_INIT_FUNCTION_NAME)){
            staticSet.remove(method.getDeclClassType());
        }

        staticClasses.addAll(staticSet);
        stmtVisitor.clear();

        this.type = type;
        this.javaMethod = method;

        this.locals = new HashSet<>(javaMethod.getBody().getLocals());
        this.stmts = new ArrayList<>(javaMethod.getBody().getStmts());
        this.thisLocal = javaMethod.getBody().getThisLocal();

        isStaticInit = TranslatorContext.STATIC_INIT_FUNCTION_NAME.equals(method.getName());
        isInit = TranslatorContext.INIT_FUNCTION_NAME.equals(method.getName());
        isIgnore = TranslatorContext.IGNORED_METHODS.contains(method.getSignature().toString());
    }

    public Set<ClassType> getStaticClasses(){
        return staticClasses;
    }

    public void setBeforeLocals(Map<Integer, Set<Local>> beforeLocals) {
        this.beforeLocals.putAll(beforeLocals);
    }

    public void setAfterLocals(Map<Integer, Set<Local>> afterLocals) {
        this.afterLocals.putAll(afterLocals);
    }

    public void setRet() {
        needRet = true;
    }

    public boolean needRet() {
        return needRet;
    }

    public Set<Local> getBeforeLocals(Stmt stmt) {
        int index = stmts.indexOf(stmt);
        return beforeLocals.get(index);
    }

    public Set<Local> getBeforeLocals(int index) {
        return beforeLocals.get(index);
    }

    public Set<Local> getAfterLocals(Stmt stmt) {
        int index = stmts.indexOf(stmt);
        return afterLocals.get(index);
    }

    public Set<Local> getAfterLocals(int index) {
        return afterLocals.get(index);
    }


    public int getFreeLabel() {
        return freeLabel;
    }

    public void setFreeLabel(int freeLabel) {
        this.freeLabel = freeLabel;
    }

    public void setLabels(List<Integer> labels) {
        this.labels.addAll(labels);
    }

    public void setBranches(Map<Integer, List<Integer>> branches) {
        this.branches.putAll(branches);
    }

    public void setReassignLocals(Set<Local> reassignLocals) {
        this.reassignLocals.addAll(reassignLocals);
    }

    public List<Integer> getBranchTargets(Stmt stmt) {
        int index = stmts.indexOf(stmt);
        return branches.get(index);
    }

    public boolean containsLabel(int index) {
        return labels.contains(index);
    }

    public String getLabelString(int index) {
        if (labels.contains(index)) {
            return "label" + labels.indexOf(index);
        } else {
            return "";
        }
    }

    public JavaSootMethod getJavaMethod() {
        return javaMethod;
    }

    public Map<Integer, Local> getParams() {
        return params;
    }

    public UDFType getUdfType() {
        return type;
    }

    public List<Stmt> getStmts() {
        return stmts;
    }

    public Local getThisLocal() {
        return this.thisLocal;
    }

    public void removeLocal(Local local){
        if(locals.contains(local)){
            this.locals.remove(local);
        }
    }

    public Set<Local> getLocals(){
        return locals;
    }

//    public void mergeLocals(Local local, Local merged){
//        // find final merged
//        Local finalMerged = merged;
//        while (mergedLocals.containsKey(finalMerged)){
//            finalMerged = mergedLocals.get(finalMerged);
//        }
//
//        mergedLocals.put(local,finalMerged);
//    }
//
//    public Local getMergedLocal(Local local){
//        return mergedLocals.getOrDefault(local, local);
//    }
//
//    public boolean isMergedLocal(Local local){
//        return mergedLocals.containsKey(local);
//    }

    public void putParams(int idx,Local local) {
        params.put(idx, local);
    }

//    public void setSimpleFree(Map<Integer, Map<LValue, Integer>> simpleFree) {
//        this.simpleFree.putAll(simpleFree);
//    }

    public void setUnknownFree(Map<Integer, Map<LValue, Integer>> unknownFree) {
        this.unknownFree.putAll(unknownFree);
    }

    public void setCircleFree(Map<Integer, Map<LValue, Integer>> circleFree) {
        this.circleFree.putAll(circleFree);
    }

    public void setIterFree(Map<Integer, LValue> iterFree) {
        this.iterFree.putAll(iterFree);
    }
    public void setGetRef(Map<Integer, Map<LValue, Integer>> getRef) {
        this.getRef.putAll(getRef);
    }
    public void setReassignStmts(Set<Integer> reassignStmts) {
        this.reassignStmts.addAll(reassignStmts);
    }

    public void setMakeNull(Map<Integer, Set<LValue>> fields) {
        this.makeNull.putAll(fields);
    }

    public void setGotoFreeStmts(Set<Integer> gotoFreeStmts) {
        this.gotoFreeStmts.addAll(gotoFreeStmts);
    }

    public void setRetStmts(Map<Integer, Immediate> retStmts) {
        this.retStmts.putAll(retStmts);
    }

//    public void setNextLabels(List<Integer> nextLabels) {
//        this.nextLabels.addAll(nextLabels);
//    }
//
//    public void setGotoNextStmts(Map<Integer, Integer> gotoNextStmts) {
//        this.gotoNextStmts.putAll(gotoNextStmts);
//    }

    public boolean isRemoved(int i) {
        return removedStmts.contains(i);
    }

    public void addRemovedStmt(int i) {
        removedStmts.add(i);
    }

    public void unRemoveStmt(int i) {
        removedStmts.remove(i);
    }

    public void addRemovedStmts(Set<Integer> removedStmts) {
        this.removedStmts.addAll(removedStmts);
    }

    public boolean isStaticInit() {
        return isStaticInit;
    }

    public boolean isInit() {
        return isInit;
    }

    public boolean isIgnore() {
        return isIgnore;
    }

//    public Map<LValue, Integer> getSimpleFreeVars(int i) {
//        if (simpleFree.containsKey(i)) {
//            return simpleFree.get(i);
//        }
//        return new HashMap<>();
//    }

    public Map<LValue, Integer> getUnknownFreeVars(int i) {
        if (unknownFree.containsKey(i)) {
            return unknownFree.get(i);
        }
        return new HashMap<>();
    }

    public Map<LValue, Integer> getCircleFreeVars(int i) {
        if (circleFree.containsKey(i)) {
            return circleFree.get(i);
        }
        return new HashMap<>();
    }

    public LValue getIterFree(int i) {
        return iterFree.get(i);
    }

    public Map<LValue, Integer> getGetRefVars(int i) {
        if (getRef.containsKey(i)) {
            return getRef.get(i);
        }
        return new HashMap<>();
    }

    public boolean isReassignStmt(int i) {
        return reassignStmts.contains(i);
    }

    public Set<Local> getReassignLocals() {
        return this.reassignLocals;
    }

    public Set<LValue> getMakeNullVars(int i) {
        if (makeNull.containsKey(i)){
            return makeNull.get(i);
        }
        return new HashSet<>();
    }

    public boolean hasGoto(int i) {
        return gotoFreeStmts.contains(i);
    }

    public boolean hasRet(int i) {
        return retStmts.containsKey(i);
    }

    public Immediate getRetValue(int i) {
        return retStmts.get(i);
    }

//    public boolean hasNextLabel(int index) {
//        return nextLabels.contains(index);
//    }
//
//    public String getNextLabel(int index) {
//        if (nextLabels.contains(index)) {
//            return "next" + nextLabels.indexOf(index);
//        } else {
//            return "";
//        }
//    }
//
//    public boolean isGotoNext(int index) {
//        return gotoNextStmts.containsKey(index);
//    }
//
//    public String getGotoNext(int index) {
//        return "goto next" + gotoNextStmts.get(index) + ";";
//    }

    public void setClearStmts(Map<Integer, Local> clearStmts) {
        this.clearStmts.putAll(clearStmts);
    }

    public Local getClearColl(int i) {
        return clearStmts.get(i);
    }

    public void setIgnoredReturnValues(Set<Integer> ignoredReturnValues) {
        this.ignoredReturnValues.addAll(ignoredReturnValues);
    }

    public boolean hasIgnoredValues() {
        return ! ignoredReturnValues.isEmpty();
    }
    public boolean isIgnoredValue(int i) {
        return ignoredReturnValues.contains(i);
    }
}
