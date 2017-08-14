#!/bin/bash -x

for file in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name hadoop-env.xml)
do
  if ! grep 'export HADOOP_CLASSPATH.*/usr/lib/hadoop/lib/\*' $file &>/dev/null; then
    sed -i 's/\(export HADOOP_CLASSPATH=.*\)/\1:\/usr\/lib\/hadoop\/lib\/*/' $file
  else
    echo "/usr/lib/hadoop/lib/ is already added to the HADOOP_CLASSPATH"
  fi
done
