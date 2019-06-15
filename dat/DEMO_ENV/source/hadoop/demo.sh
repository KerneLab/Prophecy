# All
$HADOOP_HOME/sbin/hadoop-daemon.sh start journalnode

# Master
$HADOOP_HOME/bin/hdfs namenode -format
$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs start namenode

# Backup when add/change backup namenode
$HADOOP_HOME/bin/hdfs namenode -bootstrapStandby

# Master
$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs stop namenode
# Master when add/change backup namenode
$HADOOP_HOME/bin/hdfs zkfc -formatZK

# All
$HADOOP_HOME/sbin/hadoop-daemon.sh stop journalnode

#############################################################################################

$HADOOP_HOME/sbin/start-dfs.sh
#$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs start namenode
#$HADOOP_HOME/sbin/hadoop-daemons.sh --config $HADOOP_CONF_DIR --script hdfs start datanode
#$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs start zkfc
$HADOOP_HOME/sbin/stop-dfs.sh

$HADOOP_HOME/bin/hdfs haadmin -getServiceState nn1

#############################################################################################

# Master
# Backup
$HADOOP_HOME/sbin/yarn-daemon.sh --config $HADOOP_CONF_DIR start resourcemanager

# Master
$HADOOP_HOME/sbin/yarn-daemons.sh --config $HADOOP_CONF_DIR start nodemanager

$HADOOP_HOME/bin/yarn rmadmin -getServiceState rm1

# Master
$HADOOP_HOME/sbin/yarn-daemons.sh --config $HADOOP_CONF_DIR stop nodemanager

# Master
# Backup
$HADOOP_HOME/sbin/yarn-daemon.sh --config $HADOOP_CONF_DIR stop resourcemanager

#############################################################################################

# Archive files to har
$HADOOP_HOME/bin/hadoop archive -archiveName demo.har -p /output/split/demodata /output/demoarchive

# Add new Namenode
$HADOOP_HOME/bin/hdfs dfsadmin -refreshNameNodes <datanode_host_name>:<datanode_rpc_port>
