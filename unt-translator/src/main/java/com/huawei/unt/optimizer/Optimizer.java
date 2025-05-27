/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.optimizer;

import com.huawei.unt.model.MethodContext;

/**
 * Optimizer statements before translate
 *
 * @since 2025-05-19
 */
public interface Optimizer {
    /**
     * check use this optimizer or not
     *
     * @param methodContext methodContext
     * @return use this optimizer or not
     */
    boolean fetch(MethodContext methodContext);

    /**
     * handle method
     *
     * @param methodContext methodContext
     */
    void optimize(MethodContext methodContext);
}
