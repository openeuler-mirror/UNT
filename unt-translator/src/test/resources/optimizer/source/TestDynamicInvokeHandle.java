/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * For TestDynamicInvokeHandle optimizer test
 *
 * @since 2025-08-16
 */
public class TestDynamicInvokeHandle {
    /**
     * test simple lambda case
     */
    public void testSimpleLambda() {
        List<String> list = new ArrayList<>();
        list.removeIf(s -> {
            String lowerCase = s.toLowerCase(Locale.ROOT);
            return lowerCase.startsWith("test");
        });
    }

    /**
     * test simple lambda case with context obj
     *
     * @param prefix context obj
     */
    public void testRefContextObjLambda(String prefix) {
        List<String> list = new ArrayList<>();
        list.removeIf(s -> {
            String lowerCase = s.toLowerCase(Locale.ROOT);
            return lowerCase.startsWith(prefix);
        });
    }

    /**
     * test static method ref case
     */
    public void testStaticMethodRef() {
        List<String> list = new ArrayList<>();
        list.removeIf(Boolean::getBoolean);
    }

    /**
     * test Instance Method Ref case
     */
    public void testInstanceMethodRef() {
        List<String> list = new ArrayList<>();
        list.removeIf(String::isEmpty);
    }
}