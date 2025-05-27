/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import com.huawei.unt.translator.TranslatorContext;

import com.google.common.collect.ImmutableList;

import sootup.core.transform.BodyInterceptor;
import sootup.interceptors.Aggregator;
import sootup.interceptors.CastAndReturnInliner;
import sootup.interceptors.ConstantPropagatorAndFolder;
import sootup.interceptors.CopyPropagator;
import sootup.interceptors.EmptySwitchEliminator;
import sootup.interceptors.LocalNameStandardizer;
import sootup.interceptors.LocalSplitter;
import sootup.interceptors.NopEliminator;
import sootup.interceptors.TypeAssigner;

import java.util.List;

/**
 * BaseTest
 *
 * @since 2025-05-19
 */
public abstract class BaseTest {
    /**
     * Base Test INTERCEPTORS
     */
    public static final List<BodyInterceptor> INTERCEPTORS = ImmutableList.of(new NopEliminator(),
            new EmptySwitchEliminator(), new CastAndReturnInliner(), new LocalSplitter(), new Aggregator(),
            new CopyPropagator(), new ConstantPropagatorAndFolder(), new TypeAssigner(), new LocalNameStandardizer());

    static {
        TranslatorContext.init("src/test/resources/conf");
    }
}
