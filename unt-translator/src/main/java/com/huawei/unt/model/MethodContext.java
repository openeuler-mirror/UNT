/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.model;

import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.type.UDFType;

import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.visitor.AbstractStmtVisitor;
import sootup.core.jimple.visitor.AbstractValueVisitor;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * MethodContext
 *
 * @since 2025-05-19
 */
public class MethodContext {
    private final JavaSootMethod javaMethod;

    private final UDFType type;
    private final Local thisLocal;
    private boolean needRet = false;
    private int freeLabel = -1;
    private final boolean isStaticInit;
    private final boolean isInit;
    private final boolean isIgnore;

    private final Set<Local> locals;
    private final List<Stmt> stmts;
    private final Map<Integer, Local> params = new HashMap<>();
    private final List<Integer> labels = new ArrayList<>();
    private final Map<Integer, List<Integer>> branches = new HashMap<>();
    private final HashSet<Integer> removedStmts = new HashSet<>();
    private final Map<Integer, Map<LValue, Integer>> unknownFree = new HashMap<>();
    private final Map<Integer, Map<LValue, Integer>> circleFree = new HashMap<>();
    private final Map<Integer, Map<LValue, Integer>> getRef = new HashMap<>();
    private final Set<Integer> reassignStmts = new HashSet<>();
    private final Set<Local> reassignLocals = new HashSet<>();
    private final Set<Integer> ignoredReturnValues = new HashSet<>();
    private final Map<Integer, Set<LValue>> makeNull = new HashMap<>();
    private final Set<Integer> gotoFreeStmts = new HashSet<>();
    private final Map<Integer, Immediate> retStmts = new HashMap<>();
    private final Map<Integer, Set<Local>> beforeLocals = new HashMap<>();
    private final Map<Integer, Set<Local>> afterLocals = new HashMap<>();
    private final Set<ClassType> staticClasses = new HashSet<>();

    public MethodContext(JavaSootMethod method, UDFType type) {
        StaticStmtVisitor stmtVisitor = new StaticStmtVisitor();

        for (Stmt stmt : method.getBody().getStmts()) {
            stmt.accept(stmtVisitor);
        }
        Set<ClassType> classes = stmtVisitor.getClasses();
        Set<ClassType> staticSet = classes.stream()
                .filter(c -> !TranslatorContext.getStringMap().containsKey(c.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        if (TranslatorContext.STATIC_INIT_FUNCTION_NAME.equals(method.getName())) {
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
        isIgnore = TranslatorContext.getIgnoredMethods().contains(method.getSignature().toString());
    }

    public Set<ClassType> getStaticClasses() {
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

    public Set<Local> getBeforeLocals(int index) {
        return beforeLocals.get(index);
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

    /**
     * get stmt targets
     *
     * @param stmt branch stmt
     * @return targets
     */
    public List<Integer> getBranchTargets(Stmt stmt) {
        int index = stmts.indexOf(stmt);
        return branches.get(index);
    }

    /**
     * check stmt i contains label
     *
     * @param index stmt index
     * @return stmt i has label
     */
    public boolean containsLabel(int index) {
        return labels.contains(index);
    }

    /**
     * get goto label string
     *
     * @param index index
     * @return labelString
     */
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

    /**
     * Remove some local
     *
     * @param local removed local
     */
    public void removeLocal(Local local) {
        if (locals.contains(local)) {
            this.locals.remove(local);
        }
    }

    public Set<Local> getLocals() {
        return locals;
    }

    /**
     * put params
     *
     * @param idx index
     * @param local local
     */
    public void putParams(int idx, Local local) {
        params.put(idx, local);
    }

    /**
     * Set Unknown Free
     *
     * @param unknownFree unKnownFree
     */
    public void setUnknownFree(Map<Integer, Map<LValue, Integer>> unknownFree) {
        this.unknownFree.putAll(unknownFree);
    }

    /**
     * Set circleFree
     *
     * @param circleFree circleFree
     */
    public void setCircleFree(Map<Integer, Map<LValue, Integer>> circleFree) {
        this.circleFree.putAll(circleFree);
    }

    /**
     * Set Get ref
     *
     * @param getRef getRef
     */
    public void setGetRef(Map<Integer, Map<LValue, Integer>> getRef) {
        this.getRef.putAll(getRef);
    }

    public void setReassignStmts(Set<Integer> reassignStmts) {
        this.reassignStmts.addAll(reassignStmts);
    }

    /**
     * mark some fields to null
     *
     * @param fields fields marked to null
     */
    public void setMakeNull(Map<Integer, Set<LValue>> fields) {
        this.makeNull.putAll(fields);
    }

    /**
     * set GotoFree Stmts
     *
     * @param gotoFreeStmts gotoFreeStmts
     */
    public void setGotoFreeStmts(Set<Integer> gotoFreeStmts) {
        this.gotoFreeStmts.addAll(gotoFreeStmts);
    }

    /**
     * set retStmts
     *
     * @param retStmts retStmts
     */
    public void setRetStmts(Map<Integer, Immediate> retStmts) {
        this.retStmts.putAll(retStmts);
    }

    /**
     * check stmt i is removed or not
     *
     * @param i stmt index
     * @return stmt i is removed or not
     */
    public boolean isRemoved(int i) {
        return removedStmts.contains(i);
    }

    /**
     * mark stmt i is removed
     *
     * @param i stmt index
     */
    public void addRemovedStmt(int i) {
        removedStmts.add(i);
    }

    /**
     * unRemove stmt i
     *
     * @param i stmt index
     */
    public void unRemoveStmt(int i) {
        removedStmts.remove(i);
    }

    /**
     * check if this method is static init method
     *
     * @return this method is static init method or not
     */
    public boolean isStaticInit() {
        return isStaticInit;
    }

    /**
     * check if this method is Init method
     *
     * @return this method is init method or not
     */
    public boolean isInit() {
        return isInit;
    }

    /**
     * check if this method is ignored
     *
     * @return this method is Ignored or not
     */
    public boolean isIgnore() {
        return isIgnore;
    }

    /**
     * Get Unknown free vars
     *
     * @param i unknownFree index
     * @return Unknown Free Vars
     */
    public Map<LValue, Integer> getUnknownFreeVars(int i) {
        if (unknownFree.containsKey(i)) {
            return unknownFree.get(i);
        }
        return new HashMap<>();
    }

    /**
     * Get circleFree Vars
     *
     * @param i circleFree index
     * @return circleFreeVars
     */
    public Map<LValue, Integer> getCircleFreeVars(int i) {
        if (circleFree.containsKey(i)) {
            return circleFree.get(i);
        }
        return new HashMap<>();
    }

    /**
     * Get getRef vars
     *
     * @param i ref index
     * @return GetRef map
     */
    public Map<LValue, Integer> getGetRefVars(int i) {
        if (getRef.containsKey(i)) {
            return getRef.get(i);
        }
        return new HashMap<>();
    }

    /**
     * Check it is reassignStmt
     *
     * @param i reassignStmt index
     * @return is reassignStmt
     */
    public boolean isReassignStmt(int i) {
        return reassignStmts.contains(i);
    }

    /**
     * get reassignLocals
     *
     * @return reassignLocals
     */
    public Set<Local> getReassignLocals() {
        return this.reassignLocals;
    }

    /**
     * get make null vars
     *
     * @param i index
     * @return vars
     */
    public Set<LValue> getMakeNullVars(int i) {
        if (makeNull.containsKey(i)) {
            return makeNull.get(i);
        }
        return new HashSet<>();
    }

    /**
     * check has goto free stmts
     *
     * @param i stmts index
     * @return has goto stmt
     */
    public boolean hasGoto(int i) {
        return gotoFreeStmts.contains(i);
    }

    /**
     * Check this method has ret
     *
     * @param i ret Stmts index
     * @return has ret or not
     */
    public boolean hasRet(int i) {
        return retStmts.containsKey(i);
    }

    /**
     * Return retValue
     *
     * @param i stmts index
     * @return ret value
     */
    public Immediate getRetValue(int i) {
        return retStmts.get(i);
    }

    /**
     * set ignored return values
     *
     * @param ignoredReturnValues ignored return values
     */
    public void setIgnoredReturnValues(Set<Integer> ignoredReturnValues) {
        this.ignoredReturnValues.addAll(ignoredReturnValues);
    }

    /**
     * if this method has ignored values
     *
     * @return has ignored values
     */
    public boolean hasIgnoredValues() {
        return ! ignoredReturnValues.isEmpty();
    }

    /**
     * check if ignored value
     *
     * @param i value index
     * @return value ignored or not
     */
    public boolean isIgnoredValue(int i) {
        return ignoredReturnValues.contains(i);
    }

    private static class StaticStmtVisitor extends AbstractStmtVisitor {
        private final StaticValueVisitor valueVisitor;

        public StaticStmtVisitor() {
            this.valueVisitor = new StaticValueVisitor();
        }

        /**
         * Get classes
         *
         * @return classes
         */
        public Set<ClassType> getClasses() {
            return valueVisitor.getClasses();
        }

        /**
         * clear value visitor
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

    private static class StaticValueVisitor extends AbstractValueVisitor {
        private final Set<ClassType> classes;

        public StaticValueVisitor() {
            this.classes = new HashSet<>();
        }

        public Set<ClassType> getClasses() {
            return classes;
        }

        /**
         * clear classes
         */
        public void clear() {
            this.classes.clear();
        }

        @Override
        public void caseStaticFieldRef(@Nonnull JStaticFieldRef ref) {
            if (ref.getType() instanceof ClassType && !TranslatorContext.getIgnoredClasses()
                    .contains(((ClassType) ref.getType()).getFullyQualifiedName())) {
                classes.add(ref.getFieldSignature().getDeclClassType());
            }
        }
    }
}
