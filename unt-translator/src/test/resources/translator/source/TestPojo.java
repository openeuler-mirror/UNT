package com.cutiedeng;

import org.apache.flink.api.common.functions.*;

class PojoComplex {
    private long real;
    private long img;

    public long getReal () {
        return real;
    }

    public void setReal (long real) {
        this.real = real;
    }

    public long getImg () {
        return img;
    }

    public void setImg (long img) {
        this.img = img;
    }
}

class PojoThunk {
    private int thunkId;

    public int getThunkId () {
        return thunkId;
    }

    public void setThunkId (int thunkId) {
        this.thunkId = thunkId;
    }
}

class PojoThunkComplex {
    private int thunkId;
    private long real;
    private long img;

    public long getReal () {
        return real;
    }

    public void setReal (long real) {
        this.real = real;
    }

    public int getThunkId () {
        return thunkId;
    }

    public void setThunkId (int thunkId) {
        this.thunkId = thunkId;
    }

    public long getImg () {
        return img;
    }

    public void setImg (long img) {
        this.img = img;
    }
}

class PojoSimpleUse implements MapFunction<String, String> {
    public PojoComplex c = new PojoComplex ();
    public PojoThunk t = new PojoThunk ();

    @Override
    public String map (String s) throws Exception {
        PojoThunkComplex tc = new PojoThunkComplex ();
        tc.setImg (c.getImg ());
        tc.setReal (c.getReal ());
        tc.setThunkId (t.getThunkId ());
        return tc.toString () + s;
    }
}
