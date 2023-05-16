package consumer.kafka;

import java.util.Properties;

import kafka.server.KafkaServerStartable;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.test.TestingServer;


public class KafkaTestBroker {

    private final int port = 49123;
    private KafkaServerStartable kafka;
    private TestingServer server;
    private String zookeeperConnectionString;

    public KafkaTestBroker(String indexDir) {
        try {
            server = new TestingServer();
            zookeeperConnectionString = server.getConnectString();
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
            CuratorFramework zookeeper = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
            zookeeper.start();
            Properties p = new Properties();
            p.setProperty("zookeeper.connect", zookeeperConnectionString);
            p.setProperty("broker.id", "0");
            p.setProperty("port", "" + port);
            p.setProperty("log.dir",indexDir );
            kafka.server.KafkaConfig config = new kafka.server.KafkaConfig(p);
            kafka = new KafkaServerStartable(config);
            kafka.startup();
        } catch (Exception ex) {
            throw new RuntimeException("Could not start test broker", ex);
        }
    }

    public String getZkConnectionString(){
    	
    	return zookeeperConnectionString;
    }
    public String getBrokerConnectionString() {
        return "localhost:" + port;
    }

    public int getPort() {
        return port;
    }

    public void shutdown() {
    	kafka.shutdown();
    	server.stop();
    	server.close();
    }
}
