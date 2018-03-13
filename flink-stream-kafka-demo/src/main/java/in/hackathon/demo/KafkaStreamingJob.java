package in.hackathon.demo;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.util.Collector;
import java.util.Properties;

public class KafkaStreamingJob {

	public static void main(String[] args) throws Exception {

		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", "135.242.204.108:9092");
		properties.setProperty("group.id", "test-consumer-group");

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.addSource(new FlinkKafkaConsumer010<>("test", new SimpleStringSchema(), properties))
				.flatMap(new FlatMapFunction<String, MetricsWithCount>() {
					@Override
					public void flatMap(String value, Collector<MetricsWithCount> out) {
						String[] value_array = value.split("\\s");
						out.collect(new MetricsWithCount("request", 1L));
						if(value_array.length>10 && value_array[8].equals("200")) {
							out.collect(new MetricsWithCount("throughput", value_array[9]));
						}
					}
				})
				.keyBy("word")
				.timeWindow(Time.seconds(5))
				.reduce(new ReduceFunction<MetricsWithCount>() {
					@Override
					public MetricsWithCount reduce(MetricsWithCount a, MetricsWithCount b) {
						return new MetricsWithCount(a.word, a.count + b.count);
					}
				})
				.map(new MapOutputJson())
				.addSink(new FlinkKafkaProducer010<>("telegraf", new SimpleStringSchema(), properties));

		// execute program
		env.execute("Flink Streaming with Kafka Source Java API Skeleton");
	}

	public static class MetricsWithCount {

		public String word;
		public long count;

		public MetricsWithCount() {}

		public MetricsWithCount(String word, long count) {
			this.word = word;
			this.count = count;
		}
		public MetricsWithCount(String word, String count) {
			this.word = word;
			try {
				this.count = Integer.parseInt(count);
			} catch (NumberFormatException e) {
				this.count = 0;
			}
		}

		@Override
		public String toString() {
			return "{\""+word + "\" : " + count+"}";
		}
	}

	public static class MapOutputJson implements MapFunction<MetricsWithCount, String> {
		private static final long serialVersionUID = 1L;

		@Override
		public String map(MetricsWithCount value) throws
				Exception {
			return value.toString();
		}
	}
}
