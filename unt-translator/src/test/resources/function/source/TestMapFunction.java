import org.apache.flink.api.common.functions.MapFunction;

public class TestMapFunction implements MapFunction<String, String> {
    @Override
    public String map(String input) {
        return input.replace('.', '_');
    }
}
