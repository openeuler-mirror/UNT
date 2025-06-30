/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt.translator;

import com.huawei.unt.BaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test TranslatorUtilsTest
 *
 * @since 2025-06-11
 */
public class TranslatorUtilsTest extends BaseTest {
    @Test
    public void testGetJarHashPath() {
        String expectHash = "22c6db8cf7db96d80649af5e38d9ca1fd3292dbe545d2f0c36a7e8c69826dc5f";
        String actualHash = TranslatorUtils.getJarHashPath("src/test/resources/loader/binary/loaderTestcase.jar");

        Assertions.assertTrue(expectHash.equals(actualHash));
    }

    @Test
    public void testParseSignature() {
        String signature = "Ljava/util/List<Ljava/lang/String;>;";
        String actualSignature = TranslatorUtils.parseSignature(signature);
        System.out.println(actualSignature);
    }
}
