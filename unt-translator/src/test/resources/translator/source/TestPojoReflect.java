package com.cutiedeng;

import org.apache.flink.api.common.functions.*;

class PojoDirectExecutor implements MapFunction<String, String> {
    public String pojoFieldName;
    @Override
    public String map (String s) throws Exception {
        Class<?>[] clas = new Class[] { PojoThunk.class, PojoComplex.class };
        Class<?> cla = clas[s.length () % clas.length];
        Object pojo = cla.newInstance ();
        cla.getField (pojoFieldName).setLong (pojo, 16);
        return pojo.toString ();
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

class PojoThunk {
    private int thunkId;

    public int getThunkId () {
        return thunkId;
    }

    public void setThunkId (int thunkId) {
        this.thunkId = thunkId;
    }
}

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
