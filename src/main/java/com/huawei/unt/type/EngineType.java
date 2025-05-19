package com.huawei.unt.type;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.flink.FlinkFlatMapFunction;
import com.huawei.unt.type.flink.FlinkKeySelector;
import com.huawei.unt.type.flink.FlinkMapFunction;
import com.huawei.unt.type.flink.FlinkReduceFunction;
import com.huawei.unt.type.flink.FlinkRichFilterFunction;
import com.huawei.unt.type.flink.FlinkRichFlatMapFunction;
import com.huawei.unt.type.flink.FlinkRichParallelSourceFunction;

import java.util.List;

public enum EngineType {
    FLINK;

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
                        FlinkReduceFunction.INSTANCE);
            default:
                throw new TranslatorException("Unknown engine type: " + type);
        }
    }
}
