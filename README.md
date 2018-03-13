==architecture
Streaming data generator(access logs): wrk+nginx
Streaming data collection(): flume
MQ(topic test and telegraf): kafka
Streaming data process(source from topic test and sink to topic telegraf): flink
Metric(TPS and Throughput) collection&storage&monitor: telegraf+influxdb+chronograf/grafana

==nginx 
	[root@ne3s la]# cat /etc/yum.repos.d/nginx.repo 
	# nginx.repo

	[nginx]
	name=nginx repo
	baseurl=http://nginx.org/packages/centos/7/$basearch/
	gpgcheck=0
	enabled=1
	[root@ne3s la]# 

	yum install nginx
	systemctl start nginx
	systemctl enable nginx
	
==wrk 
	git clone https://github.com/wg/wrk.git
	cd wrk
	make install

==zookeeper
cd /var/la/zookeeper-3.3.6/bin
./zkServer.sh start  

==kafka
start kafka
	cd /var/la/kafka_2.12-0.10.2.1
	./bin/kafka-server-start.sh ./config/server.properties &

create topic:
	./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
	./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic telegraf


==flume
flume configuration:
	[root@ne3s apache-flume-1.8.0-bin]# cat conf/flume.conf 
	# the Agent define
	a1.sources = r1
	a1.sinks = k1
	a1.channels = c1


	# Flume source
	a1.sources.r1.type = exec
	a1.sources.r1.command = tail -f /var/log/nginx/access.log
	a1.sources.r1.shell = /bin/sh -c
	a1.sources.r1.batchSize = 50


	# Flume sink
	#a1.sinks.k1.type = logger
	a1.sinks.k1.type = org.apache.flume.sink.kafka.KafkaSink  
	a1.sinks.k1.topic = test  
	a1.sinks.k1.brokerList = kafka-host:9092
	a1.sinks.k1.requiredAcks = 1
	a1.sinks.k1.batchSize = 50  


	# Flume channel
	a1.channels.c1.type = memory
	a1.channels.c1.capacity = 1000000
	a1.channels.c1.transactionCapacity = 100000


	# bind source and sink to channel
	a1.sources.r1.channels = c1
	a1.sinks.k1.channel = c1
	[root@ne3s apache-flume-1.8.0-bin]# 

start flume:
	cd /var/la/apache-flume-1.8.0-bin
	bin/flume-ng agent --conf conf --conf-file conf/flume.conf --name a1 -Dflume.root.logger=INFO,console &

==telegraf
	wget https://dl.influxdata.com/telegraf/releases/telegraf-1.5.2-1.x86_64.rpm
	yum localinstall telegraf-1.5.2-1.x86_64.rpm 
	systemctl start telegraf
	systemctl enable telegraf

telegraf configuration:
	# Read metrics from Kafka topic(s)
	[[inputs.kafka_consumer]]
	  ## kafka servers
	  brokers = ["kafka-host:9092"]
	  ## topic(s) to consume
	  topics = ["telegraf"]
	  name_suffix = "_nginx"

	  ## Optional SSL Config
	  # ssl_ca = "/etc/telegraf/ca.pem"
	  # ssl_cert = "/etc/telegraf/cert.pem"
	  # ssl_key = "/etc/telegraf/key.pem"
	  ## Use SSL but skip chain & host verification
	  # insecure_skip_verify = false

	  ## Optional SASL Config
	  # sasl_username = "kafka"
	  # sasl_password = "secret"

	  ## the name of the consumer group
	  consumer_group = "test-consumer-group"
	  ## Offset (must be either "oldest" or "newest")
	  offset = "oldest"

	  ## Data format to consume.
	  ## Each data format has its own unique set of configuration options, read
	  ## more about them here:
	  ## https://github.com/influxdata/telegraf/blob/master/docs/DATA_FORMATS_INPUT.md
	  data_format = "json"

	  ## Maximum length of a message to consume, in bytes (default 0/unlimited);
	  ## larger messages are dropped
	  max_message_len = 65536

==influxdb
	wget https://dl.influxdata.com/influxdb/releases/influxdb-1.4.3.x86_64.rpm
	yum localinstall influxdb-1.4.3.x86_64.rpm
	systemctl start influxdb
	systemctl enable influxdb

==chronograf/grafana
	wget https://dl.influxdata.com/chronograf/releases/chronograf-1.4.2.1.x86_64.rpm
	wget https://s3-us-west-2.amazonaws.com/grafana-releases/release/grafana-5.0.0-1.x86_64.rpm
	yum localinstall  chronograf-1.4.2.1.x86_64.rpm grafana-5.0.0-1.x86_64.rpm
	systemctl start chronograf
	systemctl enable chronograf
	
==flink
Install and start flink:
	wget http://mirrors.hust.edu.cn/apache/flink/flink-1.4.0/flink-1.4.0-bin-scala_2.11.tgz
	tar -zxvf flink-1.4.0-bin-scala_2.11.tgz
	cd flink-1.4.0/bin
	sh start-local.sh

Compile flink-stream-kafka-demo:
	mvn assembly:assembly

Copy to flink-1.4.0/bin and commit the job to flink
	sh flink.sh run flink-stream-kafka-demo-1.0-SNAPSHOT-jar-with-dependencies.jar

flink admin ui:
	http://localhost:8081/

	
	
	
	