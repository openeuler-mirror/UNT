import org.apache.flink.api.common.functions.ReduceFunction;

public class TestReduceFunction implements ReduceFunction<String> {
    @Override
    public String reduce(String s1, String s2) {
        return s1 + s2;
    }

}
