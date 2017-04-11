## Shared cluster

### Prerequisite
#### LDAP/AD
LDAP or AD is required for this setup. The easiest way to create one is to go to AWS Directory Service and create a Simple AD.
Choose an arbitrary FQDN which will be resolvable in the VPC. In this case you can only launch these cluster described below
in the same VPC that is selected here. The `Administrator` user and it's password will be required later so don't forget it.
Unfortunately the user creation process cannot be automated, so you'll need to launch a Windows 2012 instance and join it to the created
domain and then you can create users. 
To join the Windows machine to the domain: Described in the `Managing an AWS Directory Service` section here: 
https://blogs.aws.amazon.com/bigdata/post/Tx3J2RL8V6N72G7/Using-LDAP-via-AWS-Directory-Service-to-Access-and-Administer-Your-Hadoop-Enviro
To install AD tools on Windows: http://docs.aws.amazon.com/directoryservice/latest/admin-guide/install_ad_tools.html

### Create the shared cluster

It's possible to create a cluster where the Ranger and Atlas services are shared. In order to create such cluster generate
a cluster creation skeleton the following way:
```
hdc create-cluster generate-cli-skeleton --cluster-type "Enterprise Services: Apache Atlas, Apache Ranger"
```
Once the skeleton is there you need to fill the fields. Please fill the network fields as well, because it only works in 
an existing VPC and existing Subnet. 
The shared skeleton requires the `Ldap` field. In order to register and LDAP use the following command (example):
```
hdc register-ldap \
--ldap-name kriszldap \
--ldap-server ldaps://10.0.3.138:389 \
--ldap-bind-dn CN=Administrator,CN=Users,DC=ad,DC=seq,DC=com \
--ldap-bind-password Admin123! \
--ldap-domain ad.seq.com \
--ldap-user-search-base CN=Users,DC=ad,DC=seq,DC=com \
--ldap-user-search-filter CN \
--ldap-user-search-attribute sAMAccountName \
--ldap-group-search-base CN=Users,DC=ad,DC=seq,DC=com
```
The same way the hive metastore can be registered (example):
```
hdc register-metastore \
--rds-name kriszrds \
--rds-url cb.czdydiez9kxf.eu-west-1.rds.amazonaws.com \
--rds-username postgres \
--rds-password postgres \
--rds-type HIVE \
--hdp-version 2.6
```
Once it's done it should look like this (the values are for demonstration purpose only):
```
{
  "ClusterName": "krisz-datalake",
  "HDPVersion": "2.6",
  "ClusterType": "Enterprise Services: Apache Atlas, Apache Ranger",
  "Master": {
    "InstanceType": "m4.2xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 32,
    "VolumeCount": 1
  },
  "Worker": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "ephemeral",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 1
  },
  "Compute": {
      "InstanceType": "m3.xlarge",
      "VolumeType": "ephemeral",
      "VolumeSize": 40,
      "VolumeCount": 2,
      "InstanceCount": 0
    },
  "SSHKeyName": "seq-master",
  "RemoteAccess": "0.0.0.0/0",
  "WebAccess": true,
  "ClusterAndAmbariUser": "admin",
  "ClusterAndAmbariPassword": "admin",
  "InstanceRole": "CREATE",
  "Network": {
    "VpcId": "vpc-0e1dad6a",
    "SubnetId": "subnet-13b03965"
  },
  "HiveMetastore": {
    "Name": "kriszrds"
  },
  "RangerMetastore": {
    "RangerAdminPassword": "admin",
    "Name": "rangerkrisz",
    "URL": "cb.czdydiez9kxf.eu-west-1.rds.amazonaws.com:5432",
    "Username": "rangerkriszr",
    "Password": "ranger"
  },
  "CloudStoragePath": "krisz",
  "Ldap": "kriszldap"
}
```
If the LDAP is an AWS Directory Service then it must be in the same VPC as the cluster in order to access it.

If the RDS flavour for the Ranger DB is Postgres, the following commands must be executed on the Ranger DB Host, before the cluster is created. Substitute the variables in the command with the right values. RDS_DB_NAME is the database created when the RDS instance is created.
```
psql -h ${RANGER_DB_HOST} ${RDS_DB_NAME} ${RANGER_DB_ROOT_PASSWORD} 
CREATE ROLE ${RANGER_DB_USER} WITH PASSWORD '${RANGER_DB_PASSWORD}' CREATEROLE CREATEDB LOGIN;
GRANT ${RANGER_DB_USER} TO ${RANGER_DB_ROOT_USER};
```

To create the cluster:
```
hdc create-cluster --cli-input-json shared-cluster.json
```
To verify the input fields use the following command:
```
hdc describe-cluster --cluster-name krisz-shared-cluster --output json
```

### Create a connected/ephemeral cluster

To create a cluster which is connected to the previously created cluster generate a new skeleton:
```
hdc create-cluster generate-cli-skeleton --cluster-type "EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.1 Shared"
```
Note the you have to provide an extra cluster-name parameter where you specify which cluster do you want to connect to.
This will pre-fill the fields that are required to connect to the specified cluster. It must be in the same VPC and Subnet as well.
Fill the missing fields (for the HiveMetastore only the name is required):
```
{
  "ClusterName": "krisz-ephemeral-1",
  "SharedClusterName": "krisz-datalake",
  "HDPVersion": "2.6",
  "ClusterType": "EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.1 Shared",
  "Master": {
    "InstanceType": "m4.4xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 32,
    "VolumeCount": 1,
    "InstanceCount": 1,
    "Recipes": []
  },
  "Worker": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 1,
    "Recipes": [],
    "RecoveryMode": "AUTO"
  },
  "Compute": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 40,
    "VolumeCount": 1,
    "InstanceCount": 0,
    "Recipes": [],
    "RecoveryMode": "AUTO"
  },
  "SSHKeyName": "seq-master",
  "RemoteAccess": "0.0.0.0/0",
  "WebAccess": true,
  "HiveJDBCAccess": true,
  "ClusterComponentAccess": false,
  "ClusterAndAmbariUser": "admin",
  "ClusterAndAmbariPassword": "admin",
  "InstanceRole": "CREATE",
  "Network": {
    "VpcId": "vpc-0e1dad6a",
    "SubnetId": "subnet-13b03965"
  }
}

```
To create the cluster:
```
hdc create-cluster --cli-input-json connected-cluster.json
```

### Test Hive

If you go to the Ranger UI on the Shared cluster (on Ambari select the Ranger service and go to quick links, 6080 port must be open, user/pass is the same as for Ambari)
you can select the name of the cluster (krisz-shared-cluster_hive in this case) on the Hive panel you can give access to certain LDAP/AD users.
Once it's done if SSH to the either the Shared or the Connected cluster's master node you can test if the user which you granted permissions to can
access the Hive tables. To SSH to the instance you can issue the following command to get the public IP address:
```
hdc describe-cluster instances --cluster-name krisz-connected-cluster-1 --output table

+-------------+-----------------------------------------+----------------+------------+------------------------+
| INSTANCE ID |                HOSTNAME                 |   PUBLIC IP    | PRIVATE IP |          TYPE          |
+-------------+-----------------------------------------+----------------+------------+------------------------+
| i-9f50e212  | ip-10-0-2-92.eu-west-1.compute.internal | 52.211.185.122 | 10.0.2.92  | master - ambari server |
| i-d851e355  | ip-10-0-2-47.eu-west-1.compute.internal | 52.17.71.110   | 10.0.2.47  | worker                 |
| i-d951e354  | ip-10-0-2-48.eu-west-1.compute.internal | 52.49.160.178  | 10.0.2.48  | worker                 |
+-------------+-----------------------------------------+----------------+------------+------------------------+

ssh -i ~/.ssh/sequence-eu2.pem cloudbreak@52.211.185.122
```
```
beeline

beeline> !connect jdbc:hive2://localhost:10000/default
(connect with the LDAP/AD user)
Connecting to jdbc:hive2://localhost:10000/default
Enter username for jdbc:hive2://localhost:10000/default: almafa
Enter password for jdbc:hive2://localhost:10000/default: *********
Connected to: Apache Hive (version 1.2.1000.2.5.0.1-8)
Driver: Hive JDBC (version 1.2.1000.2.5.0.1-8)
Transaction isolation: TRANSACTION_REPEATABLE_READ
0: jdbc:hive2://localhost:10000/default> show tables;
+-----------+--+
| tab_name  |
+-----------+--+
+-----------+--+
No rows selected (0.116 seconds)
```
It will only print the tables if you granted access to the user in Ranger otherwise a permission denied will be printed.
