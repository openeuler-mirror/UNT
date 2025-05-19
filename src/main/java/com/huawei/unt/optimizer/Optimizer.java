package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;

public interface Optimizer {
    boolean fetch(MethodContext methodContext);

    void optimize(MethodContext methodContext);
}
