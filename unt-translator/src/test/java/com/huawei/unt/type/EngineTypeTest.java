package com.huawei.unt.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class EngineTypeTest {
    @Test
    public void testFlinkEngineType() {
        assertFalse(EngineType.getFunctions(EngineType.FLINK).isEmpty());
    }
}
