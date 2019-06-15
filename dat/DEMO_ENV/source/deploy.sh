# config and package all in port node


# ssh free login

./ssh-free-login-auto.sh '?os_user?' '?os_pass?' <!--#ssh( ):'?host?'-->

for host in <!--#ssh( ):'?host?'-->
do
scp ./ssh-free-login-auto.sh "$host":
ssh -T "$host" <<'E'OF
chmod +x ./ssh-free-login-auto.sh
./ssh-free-login-auto.sh '?os_user?' '?os_pass?' <!--#ssh( ):'?host?'-->
EOF
done

# check ssh free login
for src in <!--#ssh( ):'?host?'-->
do
ssh -T "$src" <<'E'OF
for dst in <!--#ssh( ):'?host?'-->
do
echo `hostname` to "$dst"
ssh -T "$dst" <<'E'OG
exit
EOG
done
EOF
done


# config environment

for host in <!--#Cluster( ):'?host?'-->
do
scp ~/.bash_profile ~/.bashrc ~/.etc_profile "$host":
done

##################################################################################################################
##################################################################################################################
##################################################################################################################

# zookeeper.tar.gz
<!--#Cluster():
scp zookeeper.tar.gz '?host?':?install_base?
ssh -T '?host?' <<'E'OF
cd ?install_base?
tar -xzf zookeeper.tar.gz
rm zookeeper.tar.gz
cd ?zookeeper_data?
echo ?id? >myid
EOF-->

# zookeeper start
for host in <!--#ZooKeeper( ):'?host?'-->
do
ssh -T "$host" <<'E'OF
cd ?zookeeper_base?
zkServer.sh start
rm zookeeper.out
EOF
done

##################################################################################################################
##################################################################################################################
##################################################################################################################

# hadoop.tar.gz
for host in <!--#Cluster( ):'?host?'-->
do
scp hadoop.tar.gz "$host":?install_base?
ssh -T "$host" <<'E'OF
tar -xzf hadoop.tar.gz
rm hadoop.tar.gz
EOF
done

# config JAVA_HOME in $HADOOP_CONF_DIR/hadoop_env.sh if need

# hadoop init
for host in <!--#JournalNode( ):'?host?'-->
do
ssh -T "$host" <<'E'OF
$HADOOP_HOME/sbin/hadoop-daemon.sh start journalnode
EOF
done

ssh -T <!--#NameNode:'?host?'--> <<'E'OF
$HADOOP_HOME/bin/hdfs namenode -format
$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs start namenode
EOF

i=0
for host in <!--#NameNode( ):'?host?'-->
do
if [ $i -gt 0 ]; then
ssh -T "$host" <<'E'OF
$HADOOP_HOME/bin/hdfs namenode -bootstrapStandby
EOF
fi
i=$((i+1))
done

ssh -T <!--#NameNode:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/hadoop-daemon.sh --config $HADOOP_CONF_DIR --script hdfs stop namenode
$HADOOP_HOME/bin/hdfs zkfc -formatZK
EOF

for host in <!--#JournalNode( ):'?host?'-->
do
ssh -T "$host" <<'E'OF
$HADOOP_HOME/sbin/hadoop-daemon.sh stop journalnode
EOF
done

##################################################################################################################
##################################################################################################################
##################################################################################################################

# hadoop start
ssh -T <!--#NameNode:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/start-dfs.sh
EOF


# yarn start
for host in <!--#ResourceManager( ):'?host?'-->
do
ssh -T "$host" <<'E'OF
$HADOOP_HOME/sbin/yarn-daemon.sh --config $HADOOP_CONF_DIR start resourcemanager
EOF
done

ssh -T <!--#ResourceManager:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/yarn-daemons.sh --config $HADOOP_CONF_DIR start nodemanager
EOF

ssh -T <!--#JobHistory:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/mr-jobhistory-daemon.sh start historyserver
EOF



# yarn stop
ssh -T <!--#JobHistory:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/mr-jobhistory-daemon.sh stop historyserver
EOF

ssh -T <!--#ResourceManager:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/yarn-daemons.sh --config $HADOOP_CONF_DIR stop nodemanager
EOF

for host in <!--#ResourceManager( ):'?host?'-->
do
ssh -T "$host" <<'E'OF
$HADOOP_HOME/sbin/yarn-daemon.sh --config $HADOOP_CONF_DIR stop resourcemanager
EOF
done


# hadoop stop
ssh -T <!--#NameNode:'?host?'--> <<'E'OF
$HADOOP_HOME/sbin/stop-dfs.sh
EOF
