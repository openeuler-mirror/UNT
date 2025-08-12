/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

/**
 * For MemoryReleaseOptimizer optimizer test
 *
 * @since 2025-06-10
 */
public class TestMemory {
    /**
     * test use ret as return op to deal with not only one return stmt cases
     *
     * @param in control which branch exec
     * @return res to test branch exec result
     */
    public int testNeedRet(int in) {
        if (in % 2 == 0) {
            return 7;
        } else {
            return 9;
        }
    }

    /**
     * test return op memory handle in not only one return stmt cases
     *
     * @param in control which branch exec
     * @return res to test branch exec result
     */
    public String testGotoFree(int in) {
        if (in % 2 == 0) {
            return "2n";
        } else {
            return "2n +1";
        }
    }

    /**
     * test three unknown free cases
     */
    public void testUnknownFree() {
        Person person = new Person();
        person.name = "test";
        Person[] arr = new Person[2];
        arr[0] = new Person();
    }

    /**
     * test loop free cases
     */
    public void testLoopFree() {
        for (int i = 0; i < 100; i++) {
            Person person1 = new Person();
                for (int j = i; j < 101; j++) {
                    Person person2 = new Person();
                }
            Person person3 = new Person();
        }
    }

    class Person {
        String name;
    }
}
