package lambda;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

public class lambdaTest {
    public SingleOutputStreamOperator<String> lambda() {
        DataStream<String> source = null;

        return source.map(line -> line.replace('.', '_'))
                .returns(new TypeHint<String>() {})
                .keyBy(String::hashCode)
                .reduce((s1, s2) -> s1 + s2);
    }
}
