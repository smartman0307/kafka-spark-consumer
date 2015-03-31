package consumer.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import consumer.kafka.client.KafkaRangeReceiver;
import consumer.kafka.client.KafkaReceiver;

public class ReceiverLauncher {
	
	private static String _zkPath;
	private static String _topic;

	
	public static JavaDStream<MessageAndMetadata> launch(JavaStreamingContext ssc , Properties pros, int numberOfReceivers){
		
		List<JavaDStream<MessageAndMetadata>> streamsList = new ArrayList<JavaDStream<MessageAndMetadata>>();
		JavaDStream<MessageAndMetadata> unionStreams;
		
		KafkaConfig kafkaConfig = new KafkaConfig(pros);
		ZkState zkState = new ZkState(kafkaConfig);
		
		_zkPath = (String) kafkaConfig._stateConf.get(Config.ZOOKEEPER_BROKER_PATH);
		_topic = (String) kafkaConfig._stateConf.get(Config.KAFKA_TOPIC);
		
		int numberOfPartition = getNumPartitions(zkState);
		
		//Create as many Receiver as Partition
		if(numberOfReceivers >= numberOfPartition) {
			
			for (int i = 0; i < numberOfPartition; i++) {

				streamsList.add(ssc.receiverStream(new KafkaReceiver(pros, i)));

			}
		}else {
			
			//create Range Receivers..
			Map<Integer, Set<Integer>> rMap = new HashMap<Integer, Set<Integer>>();
			
			for (int i = 0; i < numberOfPartition; i++) {

				int j = i % numberOfReceivers ;
				Set<Integer> pSet =   rMap.get(j);
				if(pSet == null) {
					pSet = new HashSet<Integer>();
					pSet.add(i);
				}else {
					pSet.add(i);
				}
				rMap.put(j, pSet);
			}
			
			for (int i = 0; i < numberOfReceivers; i++) {

				streamsList.add(ssc.receiverStream(new KafkaRangeReceiver(pros, rMap.get(i))));
			}
		}


		// Union all the streams if there is more than 1 stream
		if (streamsList.size() > 1) {
			unionStreams = ssc.union(streamsList.get(0),
					streamsList.subList(1, streamsList.size()));
		} else {
			// Otherwise, just use the 1 stream
			unionStreams = streamsList.get(0);
		}
		
		return unionStreams;
	}
	
	private static int getNumPartitions(ZkState zkState) {
		try {
			String topicBrokersPath = partitionPath();
			List<String> children = zkState.getCurator().getChildren().forPath(
					topicBrokersPath);
			return children.size();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String partitionPath() {
		return _zkPath + "/topics/" + _topic + "/partitions";
	}

}
