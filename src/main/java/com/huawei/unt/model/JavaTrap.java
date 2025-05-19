package com.huawei.unt.model;

import sootup.core.types.ClassType;

public class JavaTrap {
    private final ClassType exception;
    private final int beginIndex;
    private final int endIndex;
    private final int handleStmt;

    public JavaTrap(ClassType exception, int beginIndex, int endIndex, int handleStmt) {
        this.exception = exception;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.handleStmt = handleStmt;
    }

    public ClassType getException() {
        return exception;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getHandleStmt() {
        return handleStmt;
    }
}
