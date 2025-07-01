/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator.visitor;

import com.huawei.unt.model.MethodContext;
import com.huawei.unt.optimizer.stmts.OptimizedValue;
import com.huawei.unt.translator.TranslatorContext;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.translator.TranslatorUtils;

import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.BooleanConstant;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.Constant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.EnumConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.constant.MethodType;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JAndExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JNegExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JOrExpr;
import sootup.core.jimple.common.expr.JPhiExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JShlExpr;
import sootup.core.jimple.common.expr.JShrExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JUshrExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JXorExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.jimple.common.ref.Ref;
import sootup.core.jimple.visitor.AbstractValueVisitor;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.annotation.Nonnull;

/**
 * TranslatorValueVisitor
 *
 * @since 2025-05-19
 */
public class TranslatorValueVisitor extends AbstractValueVisitor {
    private static final String ARRAY_ELEM_GET = "%s->get(%s)";

    private final StringBuilder valueBuilder = new StringBuilder();
    private final MethodContext methodContext;

    public TranslatorValueVisitor(MethodContext methodContext) {
        this.methodContext = methodContext;
    }

    @Override
    public void caseLocal(@Nonnull Local local) {
        if (local.equals(methodContext.getJavaMethod().getBody().getThisLocal())) {
            valueBuilder.append("this");
            return;
        }

        valueBuilder.append(TranslatorUtils.formatLocalName(local));
    }

    @Override
    public void caseBooleanConstant(@Nonnull BooleanConstant constant) {
        String value = "1".equals(constant.toString()) ? "true" : "false";
        valueBuilder.append(value);
    }

    @Override
    public void caseDoubleConstant(@Nonnull DoubleConstant constant) {
        valueBuilder.append(constant.getValue());
    }

    @Override
    public void caseFloatConstant(@Nonnull FloatConstant constant) {
        valueBuilder.append(constant.getValue()).append("f");
    }

    @Override
    public void caseIntConstant(@Nonnull IntConstant constant) {
        valueBuilder.append(constant.getValue());
    }

    @Override
    public void caseLongConstant(@Nonnull LongConstant constant) {
        valueBuilder.append(constant.getValue()).append("L");
    }

    @Override
    public void caseNullConstant(@Nonnull NullConstant constant) {
        valueBuilder.append("nullptr");
    }

    @Override
    public void caseStringConstant(@Nonnull StringConstant constant) {
        String stringConstant = StringEscapeUtils.escapeJava(constant.getValue());
        valueBuilder.append("\"").append(stringConstant).append("\"");
    }

    @Override
    public void caseEnumConstant(@Nonnull EnumConstant constant) {
        valueBuilder.append(constant.getValue());
    }

    @Override
    public void caseClassConstant(@Nonnull ClassConstant constant) {
        valueBuilder.append("ClassRegistry::instance().getClass(\"").append(
                TranslatorUtils.parseSignature(constant.getValue())
                        .replace('.', '_')
                        .replace('$', '_')).append("\")");
    }

    @Override
    public void caseMethodHandle(@Nonnull MethodHandle v) {
        throw new TranslatorException("MethodHandle is not supported yet");
    }

    @Override
    public void caseMethodType(@Nonnull MethodType v) {
        throw new TranslatorException("MethodType is not supported yet");
    }

    @Override
    public void defaultCaseConstant(@Nonnull Constant v) {
        this.defaultCaseValue(v);
    }

    @Override
    public void caseArrayRef(@Nonnull JArrayRef ref) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        ref.getBase().accept(valueVisitor);
        String base = valueVisitor.toCode();
        valueVisitor.clear();
        ref.getIndex().accept(valueVisitor);
        String index = valueVisitor.toCode();
        valueBuilder.append(String.format(ARRAY_ELEM_GET, base, index));
        if (!(ref.getType() instanceof PrimitiveType)) {
            String typeStr = TranslatorTypeVisitor.getTypeString(ref.getType());
            String valueStr = valueBuilder.toString();
            clear();
            valueBuilder.append(String.format("reinterpret_cast<%s *>(%s)", typeStr, valueStr));
        }
    }

    @Override
    public void caseInstanceFieldRef(@Nonnull JInstanceFieldRef ref) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        ref.getBase().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append("->");
        valueVisitor.clear();
        valueBuilder.append(TranslatorUtils.formatFieldName(ref.getFieldSignature().getName()));
    }

    @Override
    public void caseStaticFieldRef(@Nonnull JStaticFieldRef ref) {
        String typeString = TranslatorUtils.formatType(ref.getFieldSignature().getDeclClassType());

        valueBuilder.append(typeString).append("::")
                .append(TranslatorUtils.formatFieldName(ref.getFieldSignature().getName()));
    }

    @Override
    public void caseCaughtExceptionRef(@Nonnull JCaughtExceptionRef ref) {
        valueBuilder.append("*ex");
    }

    // attention: this is a raw ptr
    @Override
    public void caseThisRef(@Nonnull JThisRef ref) {
        valueBuilder.append("this");
    }

    @Override
    public void defaultCaseRef(@Nonnull Ref ref) {
        this.defaultCaseValue(ref);
    }

    @Override
    public void caseAddExpr(@Nonnull JAddExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" + ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseAndExpr(@Nonnull JAndExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" & ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseCmpExpr(@Nonnull JCmpExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        String op1 = valueVisitor.toCode();
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        String op2 = valueVisitor.toCode();
        valueBuilder.append(String.format("%1$s > %2$s ? 1 : (%1$s == %2$s ? 0 : -1)", op1, op2));
    }

    @Override
    public void caseCmpgExpr(@Nonnull JCmpgExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        String op1 = valueVisitor.toCode();
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        String op2 = valueVisitor.toCode();
        valueBuilder.append(String.format("%1$s > %2$s ? 1 : (%1$s == %2$s ? 0 : -1)", op1, op2));
    }

    @Override
    public void caseCmplExpr(@Nonnull JCmplExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        String op1 = valueVisitor.toCode();
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        String op2 = valueVisitor.toCode();
        valueBuilder.append(String.format("%1$s > %2$s ? 1 : (%1$s == %2$s ? 0 : -1)", op1, op2));
    }

    @Override
    public void caseDivExpr(@Nonnull JDivExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" / ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseEqExpr(@Nonnull JEqExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" == ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseNeExpr(@Nonnull JNeExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" != ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseGeExpr(@Nonnull JGeExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" >= ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseGtExpr(@Nonnull JGtExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" > ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseLeExpr(@Nonnull JLeExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" <= ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseLtExpr(@Nonnull JLtExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" < ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseMulExpr(@Nonnull JMulExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" * ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseOrExpr(@Nonnull JOrExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" | ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseRemExpr(@Nonnull JRemExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" % ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseShlExpr(@Nonnull JShlExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" << ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseShrExpr(@Nonnull JShrExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" >> ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseUshrExpr(@Nonnull JUshrExpr expr) {
        String bits = expr.getOp1().getType().equals(PrimitiveType.getLong()) ? "64" : "32";

        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);

        expr.getOp1().accept(valueVisitor);
        String op1 = valueVisitor.toCode();
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        String op2 = valueVisitor.toCode();

        valueBuilder.append(String.format("static_cast<int%1$s_t>(static_cast<uint%1$s_t>(%2$s) >> %3$s)",
                bits, op1, op2));
    }

    @Override
    public void caseSubExpr(@Nonnull JSubExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" - ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }

    @Override
    public void caseXorExpr(@Nonnull JXorExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp1().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append(" ^ ");
        valueVisitor.clear();
        expr.getOp2().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
    }
    @Override
    public void caseCastExpr(@Nonnull JCastExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp().accept(valueVisitor);

        String typeString = TranslatorTypeVisitor.getTypeString(expr.getType());

        if (expr.getType() instanceof PrimitiveType) {
            valueBuilder.append("(").append(typeString).append(") ").append(valueVisitor.toCode());
        } else if (typeString.equals("String") && expr.getOp() instanceof StringConstant) {
            valueBuilder.append("new String(").append(valueVisitor.toCode()).append(")");
        } else if (isMatchedParam(expr)) {
            valueBuilder.append(valueVisitor.toCode());
        } else {
            valueBuilder.append("reinterpret_cast<")
                    .append(typeString)
                    .append(" *>(")
                    .append(valueVisitor.toCode())
                    .append(")");
        }
    }

    private boolean isMatchedParam(JCastExpr expr) {
        if (expr.getOp() instanceof Local) {
            Local local = (Local) expr.getOp();
            if (methodContext.getParamLocals().containsKey(local)) {
                return TranslatorTypeVisitor.getTypeString(methodContext.getParamLocals().get(local))
                        .equals(TranslatorTypeVisitor.getTypeString(expr.getType()));
            }
        }
        return false;
    }

    @Override
    public void caseNewExpr(@Nonnull JNewExpr expr) {
        throw new TranslatorException("New expr should be special handle");
    }

    @Override
    public void caseInstanceOfExpr(@Nonnull JInstanceOfExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp().accept(valueVisitor);
        valueBuilder.append("dynamic_cast<")
                .append(TranslatorUtils.formatType(expr.getType()))
                .append("*>(")
                .append(valueVisitor.toCode())
                .append(") != nullptr");
    }

    @Override
    public void caseVirtualInvokeExpr(@Nonnull JVirtualInvokeExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getBase().accept(valueVisitor);
        String base = valueVisitor.toCode();
        valueVisitor.clear();

        if (expr.getMethodSignature().toString().equals("<java.lang.String: int lastIndexOf(int)>")) {
            valueBuilder.append(base);
            valueBuilder.append("->lastIndexOf(std::string {(char) ");
            Immediate param = expr.getArgs().get(0);
            if (param instanceof IntConstant) {
                valueBuilder.append(((IntConstant) param).getValue());
            } else if (param instanceof Local) {
                valueBuilder.append(TranslatorUtils.formatLocalName((Local) param));
            } else {
                throw new TranslatorException("String::lastIndexOf(int) only support intConstant or local");
            }
            valueBuilder.append("})");
            return;
        }

        valueBuilder.append(base);
        valueBuilder.append("->");
        valueBuilder.append(expr.getMethodSignature().getName());
        if (TranslatorContext.isIsRegexAcc()
                && "<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>"
                    .equals(expr.getMethodSignature().toString())) {
            valueBuilder.append("_tune");
            if ("\"[^A-Za-z0-9_/.]+\"".equals(expr.getArgs().get(0).toString())) {
                valueBuilder.append("(").append(expr.getArgs().get(1)).append(")");
                return;
            }
        }
        valueBuilder.append(TranslatorUtils.paramsToString(expr.getMethodSignature(), expr.getArgs(), methodContext));
    }

    @Override
    public void caseSpecialInvokeExpr(@Nonnull JSpecialInvokeExpr expr) {
        String base = expr.getBase().equals(methodContext.getThisLocal()) ? "this"
                : TranslatorUtils.formatLocalName(expr.getBase());

        valueBuilder.append(base).append("->");
        valueBuilder.append(expr.getMethodSignature().getName())
                .append(TranslatorUtils.paramsToString(expr.getMethodSignature(), expr.getArgs(), methodContext));
    }

    @Override
    public void caseInterfaceInvokeExpr(@Nonnull JInterfaceInvokeExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getBase().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode());
        valueVisitor.clear();
        valueBuilder.append("->");
        valueBuilder.append(expr.getMethodSignature().getName())
                .append(TranslatorUtils.paramsToString(expr.getMethodSignature(), expr.getArgs(), methodContext));
    }

    @Override
    public void caseStaticInvokeExpr(@Nonnull JStaticInvokeExpr expr) {
        String staticFunction = TranslatorTypeVisitor.getTypeString(expr.getMethodSignature().getDeclClassType())
                + "::" + expr.getMethodSignature().getName();
        if (TranslatorContext.isIsMemTune()
                && "Long::valueOf".equals(staticFunction)) {
            staticFunction = staticFunction + "_tune";
        }
        valueBuilder.append(TranslatorUtils.formatFunctionName(staticFunction))
                .append(TranslatorUtils.paramsToString(expr.getMethodSignature(), expr.getArgs(), methodContext));
    }

    @Override
    public void caseDynamicInvokeExpr(@Nonnull JDynamicInvokeExpr expr) {
        throw new TranslatorException("DynamicInvokeExpr translator is supported limited");
    }

    @Override
    public void caseLengthExpr(@Nonnull JLengthExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp().accept(valueVisitor);
        valueBuilder.append(valueVisitor.toCode()).append("->size()");
    }

    @Override
    public void caseNegExpr(@Nonnull JNegExpr expr) {
        TranslatorValueVisitor valueVisitor = new TranslatorValueVisitor(methodContext);
        expr.getOp().accept(valueVisitor);
        String operand = expr.getOp().getType().equals(PrimitiveType.getBoolean()) ? "! " : "- ";
        valueBuilder.append(operand).append(valueVisitor.toCode());
    }

    @Override
    public void casePhiExpr(JPhiExpr expr) {
        super.casePhiExpr(expr);
    }

    @Override
    public void caseNewArrayExpr(@Nonnull JNewArrayExpr expr) {
        TranslatorValueVisitor visitor = new TranslatorValueVisitor(methodContext);
        expr.getSize().accept(visitor);
        String size = visitor.toCode();
        Type baseType = expr.getBaseType();
        if (baseType instanceof PrimitiveType) {
            valueBuilder.append(String.format("new JavaArray<%s>(%s)", baseType, size));
        } else {
            valueBuilder.append("new Array()");
        }
    }

    @Override
    public void caseNewMultiArrayExpr(@Nonnull JNewMultiArrayExpr expr) {
        throw new TranslatorException("NewMultiArrayExpr is not supported now.");
    }

    @Override
    public void defaultCaseValue(@Nonnull Value v) {
        if (v instanceof OptimizedValue) {
            valueBuilder.append(((OptimizedValue) v).getCode());
            return;
        }

        throw new TranslatorException("Unsupported value: " + v);
    }

    /**
     * clear translate string
     */
    public void clear() {
        valueBuilder.delete(0, valueBuilder.length());
    }

    /**
     * Return translate result string
     *
     * @return cpp code
     */
    public String toCode() {
        return valueBuilder.toString();
    }
}
