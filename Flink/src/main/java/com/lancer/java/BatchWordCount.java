package com.lancer.java;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.util.Collector;


public class BatchWordCount {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        // source
        DataSource<String> source = env.readTextFile("./Flink/data/word.txt");

        // transformation
        source
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String s, Collector<String> collector) throws Exception {
                        String[] words = s.split(" ");
                        for (String word : words) {
                            collector.collect(word);
                        }
                    }
                })
                .filter(StringUtils::isNoneEmpty)
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String s) throws Exception {
                        return new Tuple2<>(s, 1);
                    }
                })
                .groupBy(0)
                .sum(1)
                .sortPartition(1, Order.DESCENDING)
                //sink
                // .print();
                .writeAsText("./Flink/output", FileSystem.WriteMode.OVERWRITE);

        env.execute();
    }
}
