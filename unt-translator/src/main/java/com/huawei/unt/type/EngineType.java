/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.type;

import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.flink.FlinkFlatMapFunction;
import com.huawei.unt.type.flink.FlinkKeySelector;
import com.huawei.unt.type.flink.FlinkKeyedCoProcessFunction;
import com.huawei.unt.type.flink.FlinkMapFunction;
import com.huawei.unt.type.flink.FlinkReduceFunction;
import com.huawei.unt.type.flink.FlinkRichFilterFunction;
import com.huawei.unt.type.flink.FlinkRichFlatMapFunction;
import com.huawei.unt.type.flink.FlinkRichParallelSourceFunction;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * EngineType
 *
 * @since 2025-05-19
 */
public enum EngineType {
    FLINK;

    /**
     * Get all udfType by EngineType
     *
     * @param type engineType
     * @return all UDFType
     */
    public static List<UDFType> getFunctions(EngineType type) {
        switch (type) {
            case FLINK:
                return ImmutableList.of(
                        FlinkRichParallelSourceFunction.INSTANCE,
                        FlinkKeySelector.INSTANCE,
                        FlinkRichFlatMapFunction.INSTANCE,
                        FlinkFlatMapFunction.INSTANCE,
                        FlinkMapFunction.INSTANCE,
                        FlinkRichFilterFunction.INSTANCE,
                        FlinkReduceFunction.INSTANCE,
                        FlinkKeyedCoProcessFunction.INSTANCE);
            default:
                throw new TranslatorException("Unknown engine type: " + type);
        }
    }
}
