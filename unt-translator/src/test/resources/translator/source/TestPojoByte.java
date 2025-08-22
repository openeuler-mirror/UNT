package com.cutiedeng;

import org.apache.flink.api.common.functions.*;

class PojoByteEmitter implements MapFunction<Object, PojoByte> {
    @Override
    public PojoByte map (Object o) throws Exception {
        return new PojoByte ();
    }
}

class PojoByte {
    private byte b;
    private static byte staticB;

    public byte getB () {
        return b;
    }

    public void setB (byte b) {
        this.b = b;
    }

    public static byte getStaticB () {
        return staticB;
    }

    public static void setStaticB (byte staticB) {
        PojoByte.staticB = staticB;
    }
}
