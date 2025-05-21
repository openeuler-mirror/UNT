package functions;

import org.apache.flink.api.common.functions.MapFunction;

public class TestMapFunction implements MapFunction<String, String> {
    @Override
    public String map(String s) throws Exception {
        return s.replace('.', '_');
    }
}
