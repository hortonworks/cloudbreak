##Kerberos security

Ambari supports Kerberos security for internal communication. To activate Kerberos with Cloudbreak you have enable security option and fill the `kerberos master key`, `kerberos admin` and `kerberos password` too.
To run a job on the cluster, you can use one of the default Hadoop users, like `ambari-qa`, as usual.

**Optional**

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
```
