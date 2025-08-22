/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.cutiedeng;

import org.apache.flink.api.common.functions.*;

class PojoParseBoolean implements MapFunction<String, PojoBoolean> {

    @Override
    public PojoBoolean map (String s) throws Exception {
        if (s.equals ("true")) {
            PojoBoolean pb = new PojoBoolean();
            pb.setBooleanContent (true);
            return pb;
        } else if (s.equals ("false")) {
            PojoBoolean pb = new PojoBoolean();
            pb.setBooleanContent (true);
            return pb;
        }
        PojoBoolean pb = new PojoBoolean ();
        pb.setBooleanContent (false);
        return pb;
    }
}

class PojoBoolean {
    private boolean booleanContent;

    public boolean isBooleanContent () {
        return booleanContent;
    }

    public void setBooleanContent (boolean booleanContent) {
        this.booleanContent = booleanContent;
    }
}
