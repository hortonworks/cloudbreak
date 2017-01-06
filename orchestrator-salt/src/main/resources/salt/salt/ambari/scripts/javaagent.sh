#!/bin/bash -x

for file in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name hadoop-env.xml)
do
  sed -i 's/${HADOOP_NAMENODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=20103:\/etc\/jmx_exporter\/jmx_exporter.yml/g' $file
  sed -i 's/${HADOOP_DATANODE_OPTS}/-javaagent:\/opt\/jmx_javaagent.jar=20104:\/etc\/jmx_exporter\/jmx_exporter.yml/g' $file
done