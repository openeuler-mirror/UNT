/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Test EngineType
 *
 * @since 2025-05-19
 */
public class EngineTypeTest {
    @Test
    public void testFlinkEngineType() {
        assertFalse(EngineType.getFunctions(EngineType.FLINK).isEmpty());
    }
}
