#Kerberos security

Cloudbreak supports Kerberos security for Ambari internal communication. To activate Kerberos with Cloudbreak you should enable security option and fill the

 * `kerberos master key`
 * `kerberos admin`
 * `kerberos password`

fields too on web interface or shell during cluster creation. To run a job on the cluster, you can use one of the default Hadoop users, like `ambari-qa`, as usual.

### Test
Once kerberos is enabled you need a `ticket` to execute any job on the cluster. Here's an example to get a ticket:
```
kinit -V -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa-sparktest-rec@NODE.DC1.CONSUL
```
Example job:
```java
export HADOOP_LIBS=/usr/hdp/current/hadoop-mapreduce-client
export JAR_EXAMPLES=$HADOOP_LIBS/hadoop-mapreduce-examples.jar
export JAR_JOBCLIENT=$HADOOP_LIBS/hadoop-mapreduce-client-jobclient.jar

hadoop jar $JAR_EXAMPLES teragen 10000000 /user/ambari-qa/terasort-input

hadoop jar $JAR_JOBCLIENT mrbench -baseDir /user/ambari-qa/smallJobsBenchmark -numRuns 5 -maps 10 -reduces 5 -inputLines 10 -inputType ascending
```

### Create/add custom users

To create custom users please follow the steps below.

  * Log in via SSH to the Cloudbreak gateway node (IP address is the same as the Ambari UI)

```
sudo docker exec -i kerberos bash
kadmin -p [admin_user]/[admin_user]@NODE.DC1.CONSUL (type admin password)
addprinc custom-user (type user password twice)
```

  * Log in via SSH to all other nodes

```
sudo docker exec -i $(docker ps | grep ambari-warmup | cut -d" " -f 1) bash
useradd custom-user
```

  * Log in via SSH to one of the nodes

```
sudo docker exec -i $(docker ps | grep ambari-warmup | cut -d" " -f 1) bash
su custom-user
kinit -p custom-user (type user password)
hdfs dfs -mkdir input
hdfs dfs -put /tmp/wait-for-host-number.sh input
yarn jar $(find /usr/hdp -name hadoop-mapreduce-examples.jar) wordcount input output
hdfs dfs -cat output/*



**Note** Current implementation of Kerberos security doesn't contain Active Directory support or any other third party user authentication method. If you want to use custom user, you have to create users manually with the same name on all Ambari containers on each node.
