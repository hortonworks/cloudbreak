#!/bin/bash -x

for file in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name hadoop-env.xml)
do
  sed -i 's/${HADOOP_NAMENODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20103:\/etc\/jmx_exporter\/namenode.yml/g' $file
  sed -i 's/${HADOOP_DATANODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=127.0.0.1:20104:\/etc\/jmx_exporter\/datanode.yml/g' $file
done