/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedDirectStmt;
import com.huawei.unt.optimizer.stmts.OptimizedJAssignStmt;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;
import com.huawei.unt.translator.visitor.TranslatorTypeVisitor;
import com.huawei.unt.translator.visitor.TranslatorValueVisitor;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;

/**
 * Deal with array field
 *
 * @since 2025-05-19
 */
public class ArrayFieldHandler implements Optimizer {
    @Override
    public boolean fetch(MethodContext methodContext) {
        return true;
    }

    @Override
    public void optimize(MethodContext methodContext) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        for (int i = 0; i < methodContext.getStmts().size(); i++) {
            Stmt stmt = methodContext.getStmts().get(i);
            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getLeftOp() instanceof JArrayRef) {
                JArrayRef arrayRef = (JArrayRef) ((JAssignStmt) stmt).getLeftOp();
                Local base = arrayRef.getBase();

                String res = base + "->append(" + ((JAssignStmt) stmt).getRightOp() + ")";
                OptimizedDirectStmt optimizedDirectStmt = new OptimizedDirectStmt(res, stmt);

                methodContext.getStmts().set(i, optimizedDirectStmt);
            }

            if (stmt instanceof JAssignStmt && ((JAssignStmt) stmt).getRightOp() instanceof JArrayRef) {
                JArrayRef arrayRef = (JArrayRef) ((JAssignStmt) stmt).getRightOp();
                String localName = TranslatorUtils.formatLocalName(arrayRef.getBase());
                arrayRef.getIndex().accept(valueVisitor);
                String index = valueVisitor.toCode();
                valueVisitor.clear();

                Type type = arrayRef.getType();

                if (type instanceof PrimitiveType) {
                    throw new TranslatorException("Array is only support primary type yet");
                }

                String typeString = TranslatorTypeVisitor.getTypeString(type);
                String ref = localName + "->get(" + index + ")";

                String rightString = "reinterpret_cast<" + typeString + " *>(" + ref + ")";
                OptimizedValue rightValue = new OptimizedValue(rightString, arrayRef);

                if (methodContext.getStmts().get(i) instanceof OptimizedJAssignStmt) {
                    OptimizedJAssignStmt originalStmt = (OptimizedJAssignStmt) methodContext.getStmts().get(i);
                    Value left = originalStmt.getLeftValue();
                    JAssignStmt originalAssign = originalStmt.getOriginalStmt();
                    methodContext.getStmts().set(i, new OptimizedJAssignStmt(left, rightValue, originalAssign));
                } else {
                    methodContext.getStmts().set(i, new OptimizedJAssignStmt(((JAssignStmt) stmt).getLeftOp(),
                            rightValue, (JAssignStmt) stmt));
                }
            }
        }
    }
}
