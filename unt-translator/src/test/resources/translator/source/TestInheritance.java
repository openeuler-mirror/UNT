/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

import org.apache.flink.api.common.functions.MapFunction;

/**
 * Test TestInheritance
 *
 * @since 2025-06-11
 */
public class TestInheritance implements MapFunction<String, String> {
    @Override
    public String map(String s) throws Exception {
        return s.replace('.', '_');
    }
}
