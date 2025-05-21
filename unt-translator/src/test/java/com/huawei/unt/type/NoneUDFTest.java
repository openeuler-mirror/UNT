package com.huawei.unt.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class NoneUDFTest {
    @Test
    public void testGetBaseClass() {
        assertNull(NoneUDF.INSTANCE.getBaseClass());
    }
}
