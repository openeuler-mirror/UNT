/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test NoneUDF
 *
 * @since 2025-05-19
 */
public class NoneUDFTest {
    @Test
    public void testGetBaseClass() {
        assertEquals(NoneUDF.INSTANCE.getBaseClass(), Object.class);
    }
}
