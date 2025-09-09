/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.cutiedeng;

import org.apache.flink.api.common.functions.*;

import java.util.*;

class CharTree {
    public Map<Character, CharTree> child;
    public PojoChar value;
}

class PojoCharExtender implements MapFunction<Character, CharTree> {
    public CharTree current;

    @Override
    public CharTree map (Character character) throws Exception {
        CharTree newCharTree = new CharTree();
        current.child.put(character, newCharTree);
        current = newCharTree;
        current.value = new PojoChar ();
        current.value.setC (character);
        return newCharTree;
    }
}

class PojoChar {
    private char c;

    public char getC () {
        return c;
    }

    public void setC (char c) {
        this.c = c;
    }
}
