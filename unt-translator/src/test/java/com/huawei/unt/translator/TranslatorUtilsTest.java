package com.huawei.unt.translator;

import com.huawei.unt.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TranslatorUtilsTest extends BaseTest {
    @Test
    public void testGetJarHashPath(){
        String expectHash = "22c6db8cf7db96d80649af5e38d9ca1fd3292dbe545d2f0c36a7e8c69826dc5f";
        String jarPath = new String("src/test/resources/loader/binary/loaderTestcase.jar");
        String actualHash = TranslatorUtils.getJarHashPath(jarPath);

        Assertions.assertTrue(expectHash.equals(actualHash));
    }
}
