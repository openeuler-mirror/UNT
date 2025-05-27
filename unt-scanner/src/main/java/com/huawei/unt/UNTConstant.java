/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * unt-scanner Constant
 *
 * @since 2025-05-19
 */
public class UNTConstant {
    /**
     * All flink udf classes
     */
    public static final Set<String> FLINK_UDF_CLASSES = ImmutableSet.of(
            "org.apache.flink.api.common.functions.AbstractRichFunction",
            "org.apache.flink.api.common.functions.AggregateFunction",
            "org.apache.flink.api.common.functions.CoGroupFunction",
            "org.apache.flink.api.common.functions.CombineFunction",
            "org.apache.flink.api.common.functions.CrossFunction",
            "org.apache.flink.api.common.functions.FilterFunction",
            "org.apache.flink.api.common.functions.FlatJoinFunction",
            "org.apache.flink.api.common.functions.FlatMapFunction",
            "org.apache.flink.api.common.functions.Function",
            "org.apache.flink.api.common.functions.GroupCombineFunction",
            "org.apache.flink.api.common.functions.GroupReduceFunction",
            "org.apache.flink.api.common.functions.JoinFunction",
            "org.apache.flink.api.common.functions.MapFunction",
            "org.apache.flink.api.common.functions.MapPartitionFunction",
            "org.apache.flink.api.common.functions.ReduceFunction",
            "org.apache.flink.api.common.functions.RichAggregateFunction",
            "org.apache.flink.api.common.functions.RichCoGroupFunction",
            "org.apache.flink.api.common.functions.RichCrossFunction",
            "org.apache.flink.api.common.functions.RichFilterFunction",
            "org.apache.flink.api.common.functions.RichFlatMapFunction",
            "org.apache.flink.api.common.functions.RichFunction",
            "org.apache.flink.api.common.functions.RichGroupCombineFunction",
            "org.apache.flink.api.common.functions.RichGroupReduceFunction",
            "org.apache.flink.api.common.functions.RichJoinFunction",
            "org.apache.flink.api.common.functions.RichMapFunction",
            "org.apache.flink.api.common.functions.RichMapPartitionFunction",
            "org.apache.flink.api.common.functions.RichReduceFunction",
            "org.apache.flink.streaming.api.functions.aggregation.AggregationFunction",
            "org.apache.flink.streaming.api.functions.aggregation.ComparableAggregator",
            "org.apache.flink.streaming.api.functions.aggregation.SumAggregator",
            "org.apache.flink.streaming.api.functions.async.AsyncFunction",
            "org.apache.flink.streaming.api.functions.async.RichAsyncFunction",
            "org.apache.flink.streaming.api.functions.co.BaseBroadcastProcessFunction",
            "org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction",
            "org.apache.flink.streaming.api.functions.co.CoFlatMapFunction",
            "org.apache.flink.streaming.api.functions.co.CoMapFunction",
            "org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction",
            "org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction",
            "org.apache.flink.streaming.api.functions.co.ProcessJoinFunction",
            "org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction",
            "org.apache.flink.streaming.api.functions.co.RichCoMapFunction",
            "org.apache.flink.streaming.api.functions.sink.DiscardingSink",
            "org.apache.flink.streaming.api.functions.sink.PrintSinkFunction",
            "org.apache.flink.streaming.api.functions.sink.RichSinkFunction",
            "org.apache.flink.streaming.api.functions.sink.SinkFunction",
            "org.apache.flink.streaming.api.functions.sink.SocketClientSink",
            "org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction",
            "org.apache.flink.streaming.api.functions.source.ContinuousFileMonitoringFunction",
            "org.apache.flink.streaming.api.functions.source.FromElementsFunction",
            "org.apache.flink.streaming.api.functions.source.FromIteratorFunction",
            "org.apache.flink.streaming.api.functions.source.FromSplittableIteratorFunction",
            "org.apache.flink.streaming.api.functions.source.FileReadFunction",
            "org.apache.flink.streaming.api.functions.source.InputFormatSourceFunction",
            "org.apache.flink.streaming.api.functions.source.ParallelSourceFunction",
            "org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction",
            "org.apache.flink.streaming.api.functions.source.RichSourceFunction",
            "org.apache.flink.streaming.api.functions.source.SocketTextStreamFunction",
            "org.apache.flink.streaming.api.functions.source.SourceFunction",
            "org.apache.flink.streaming.api.functions.source.StatefulSequenceSource",
            "org.apache.flink.streaming.api.functions.windowing.AggregateApplyAllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.AggregateApplyWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.AllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.PassThroughAllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.PassThroughWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.ReduceApplyAllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.ReduceApplyProcessAllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.ReduceApplyProcessWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.ReduceApplyWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.RichAllWindowFunction",
            "org.apache.flink.streaming.api.functions.windowing.WindowFunction",
            "org.apache.flink.streaming.api.functions.KeyedProcessFunction",
            "org.apache.flink.streaming.api.functions.ProcessFunction");
}
