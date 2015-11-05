# Insights

##Cloudbreak deployer

###Debug

If you want to have more detailed output set the `DEBUG` env variable to non-zero:

```
DEBUG=1 cbd some_command
```

###Troubleshoot

You can use the `doctor` command to diagnose your environment.
It can reveal some common problems with your docker or boot2docker configuration and it also checks the cbd versions.

```
cbd doctor
```

###Logs

The aggregated logs of all the Cloudbreak components can be checked with:

```
cbd logs
```

It can also be used to check the logs of an individual docker container. To see only the logs of the Cloudbreak backend:

```
cbd logs cloudbreak
```

You can also check the individual logs of `uluwatu`, `periscope`, and `identity`.

###Update

The cloudbreak-deployer tool is capable of upgrading itself to a newer version.

```
cbd update
```

---

##Cloudbreak application

###SSH to the hosts

In the current version of Cloudbreak all the nodes have a public IP address and all the nodes are accessible via SSH.
The public IP addresses of a running cluster can be checked on the Cloudbreak UI under the *Nodes* tab.
Only key-based authentication is supported - the public key can be specified when creating a credential.

```
ssh -i ~/.ssh/private-key.pem cloudbreak@<public-ip>
```

The default user is `cloudbreak` except on EC2 where it is `ec2-user`.

###Accessing HDP client services

The main difference between general HDP clusters and Cloudbreak-installed HDP clusters is that each host runs an Ambari server or agent Docker container and the HDP services will be installed in this container as well.
It means that after `ssh` the client services won't be available instantly, first you'll have to enter the ambari-agent container.
Inside the container everything works the same way as expected.
To check the containers running on the host enter:

```
[cloudbreak@vmhostgroupclient11 ~]$ sudo docker ps
CONTAINER ID   IMAGE                                    COMMAND                CREATED      STATUS      PORTS     NAMES
1098ca778176   sequenceiq/baywatch-client:v1.0.0        "/etc/bootstrap.sh -d" 4 hours ago  Up 4 hours            baywatch-client-14454170059514
f4097c52fda5   sequenceiq/logrotate:v0.5.1              "/start.sh"            4 hours ago  Up 4 hours            logrotate-14454169954830
7b94aedaab30   sequenceiq/docker-consul-watch-plugn:1.0 "/start.sh consul://1" 4 hours ago  Up 4 hours            consul-watch-14454169884044
d8128b001427   sequenceiq/ambari:2.1.2-v2               "/start-agent"         4 hours ago  Up 4 hours            ambari-agent-14454169805924
a8ec90037aaf   swarm:0.4.0                              "/swarm join --addr=1" 4 hours ago  Up 4 hours  2375/tcp  vmhostgroupmaster12-swarm-agent
ef02b43eacee   sequenceiq/consul:v0.5.0-v5              "/bin/start"           4 hours ago  Up 4 hours            vmhostgroupmaster12-consul
```

You should see the ambari-agent container running. Copy its id or name and `exec` into the container:

```
[cloudbreak@vmhostgroupclient11 ~]$ sudo docker exec -it ambari-agent-14454169805924 bash
[root@docker-ambari tmp]#
```

Or you can use this one-step command as well:

```
[cloudbreak@vmhostgroupclient11 ~]$ sudo docker exec -it $(sudo docker ps -f=name=ambari-agent -q) bash
[root@docker-ambari tmp]#
```

###Data volumes

The disks that are attached to the instances are automatically mounted to `/hadoopfs/fs1`, `/hadoopfs/fs2`, ... `/hadoopfs/fsN` respectively.
These directories are mounted from the host into the ambari-agent container under the same name so these can be accessed from inside.
It means that if you'd like to move some data to the instances you can use these volumes and the data will be available from the container instantly to work on it.

An `scp` Example:

```
$ scp -qr -i ~/.ssh/private-key.pem ~/tmp/data cloudbreak@<client-node>:/hadoopfs/fs1
$ ssh -i ~/.ssh/private-key.pem cloudbreak@<client-node>
[cloudbreak@vmhostgroupclient11 ~]$ sudo docker exec -it $(sudo docker ps -f=name=ambari-agent -q) bash
[root@docker-ambari tmp]# su hdfs
[hdfs@docker-ambari tmp]# hadoop fs -put /hadoopfs/fs1/data /tmp
[hdfs@docker-ambari tmp]# hadoop fs -ls /tmp
Found 2 items
drwxr-xr-x   - hdfs supergroup          0 2015-10-21 13:46 /tmp/data
drwx-wx-wx   - hive supergroup          0 2015-10-21 08:51 /tmp/hive
```

###Internal hostnames

After a cluster is created with Cloudbreak, the nodes will have internal hostnames like this:

 ```vmhostgroupclient11.node.dc1.consul```

This is because Cloudbreak uses [Consul](https://www.consul.io) to provide DNS services.
It means that you won't see entries to the other nodes inside the `/etc/hosts` file, because nodes are registered inside Consul and the hostnames are resolved by Consul as well.

In the current version the `node.dc1.consul` domain is hardcoded and cannot be changed.

###Accessing Ambari server from the other nodes

Ambari server is registered as a service in Consul, so it can always be accessed through its domain name `ambari-8080.service.consul` from the other ambari containers.
It can be tried by pinging it from one of the `ambari-agent` containers:

```
ping ambari-8080.service.consul
```

###Cloudbreak gateway node

With every Cloudbreak cluster installation there is a special node called *cbgateway* started that won't run an ambari-agent container so it won't run HDP services either.
It can be seen on the Cloudbreak UI among the hostgroups when creating a cluster, but its node count cannot be changed from 1 and it shouldn't be there in the Ambari blueprint.
It is by design because this instance has some special tasks:

- it runs the Ambari server and its database inside Docker containers
- it runs an nginx proxy that is used by the Cloudbreak API to communicate with the cluster securely
- it runs the Swarm manager that orchestrates the Docker containers on the whole cluster
- it runs the Baywatch server that is responsible for collecting the operational logs from the cluster
- it runs a Kerberos KDC container if Kerberos is configured

###Hadoop logs

Hadoop logs are available from the host and from the container as well in the `/hadoopfs/fs1/logs` directory.

###Ambari db

Ambari's database runs on the `cbgateway` node inside a PostgreSQL docker container. To access it ssh to the gateway node and run the following command:

```
[cloudbreak@vmcbgateway0 ~]$ sudo docker exec -it ambari_db psql -U postgres
```