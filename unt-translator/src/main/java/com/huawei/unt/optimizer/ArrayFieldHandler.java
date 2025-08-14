/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedDirectStmt;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.PrimitiveType;

/**
 * Deal with array field
 *
 * @since 2025-05-19
 */
public class ArrayFieldHandler implements Optimizer {
    private static final String ARRAY_ELEM_SET = "%s->append(%s)";
    private static final String JAVA_ARRAY_ELEM_SET = "%s->set(%s, %s)";

    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }
    @Override
    public void optimize(MethodContext methodContext) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            // deal with arr[i] = ...
            Stmt stmt = methodContext.getStmts().get(i);
            Stmt originalStmt = stmt instanceof OptimizedJAssignStmt
                    ? ((OptimizedJAssignStmt) stmt).getOriginalStmt() : stmt;
            if (originalStmt instanceof JAssignStmt && ((JAssignStmt) originalStmt).getLeftOp() instanceof JArrayRef) {
                JArrayRef arrayRef = (JArrayRef) ((JAssignStmt) originalStmt).getLeftOp();
                Local base = arrayRef.getBase();
                Value rightValue = ((JAssignStmt) originalStmt).getRightOp();
                if (stmt instanceof OptimizedJAssignStmt) {
                    rightValue = ((OptimizedJAssignStmt) stmt).getRightValue();
                }
                rightValue.accept(valueVisitor);
                String res;
                if (arrayRef.getType() instanceof PrimitiveType) {
                    res = String.format(JAVA_ARRAY_ELEM_SET, base, arrayRef.getIndex(), valueVisitor.toCode());
                } else {
                    res = String.format(ARRAY_ELEM_SET, base, valueVisitor.toCode());
                }
                valueVisitor.clear();
                OptimizedDirectStmt optimizedDirectStmt = new OptimizedDirectStmt(res, stmt);
                methodContext.getStmts().set(i, optimizedDirectStmt);
            }
        }
    }
}
