#!/bin/bash -x

# namenode, datanode
for file in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name hadoop-env.xml)
do
  sed -i 's/${HADOOP_NAMENODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20103:\/etc\/jmx_exporter\/namenode.yml/g' $file
  sed -i 's/${HADOOP_DATANODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20104:\/etc\/jmx_exporter\/datanode.yml/g' $file
done

#hiveserver2, hive metastore
for file in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name hive-env.xml)
do
  sed -i "s/{% endif %}/{% endif %}\nif [ \"\$SERVICE\" = \"metastore\" ]; then export HADOOP_OPTS=\"\$HADOOP_OPTS -javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20107:\/etc\/jmx_exporter\/hivemetastore.yml\"; fi/" $file
  sed -i "s/{% endif %}/{% endif %}\nif [ \"\$SERVICE\" = \"hiveserver2\" ]; then export HADOOP_OPTS=\"\$HADOOP_OPTS -javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20105:\/etc\/jmx_exporter\/hiveserver2.yml\"; fi/" $file
done