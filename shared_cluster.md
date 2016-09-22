## Shared cluster

### Create the shared cluster

It's possible to create a cluster where the Ranger and Atlas services are shared. In order to create such cluster generate
a cluster creation skeleton the following way:
```
hdc create-cluster generate-cli-shared-skeleton --cluster-type "Shared"
```
Once the skeleton is there you need to fill the fields. Please fill the network fields as well, because it only works in 
an existing VPC and existing Subnet. Once it's done it should look like this (the values are for demonstration purpose only):
```
{
  "ClusterName": "krisz-shared-cluster",
  "HDPVersion": "2.5",
  "ClusterType": "Shared",
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
    "Name": "shared-metastore",
    "Username": "hiveadmin",
    "Password": "hiveadmin",
    "URL": "metastore-eu.czdydiez9kxf.eu-west-1.rds.amazonaws.com:5432/hivedb",
    "DatabaseType": "POSTGRES"
  },
  "ClusterInputs": {
    "LDAP_BIND_DN": "CN=Administrator,CN=Users,DC=ad,DC=seq,DC=com",
    "LDAP_BIND_PASSWORD": "Admin123!",
    "LDAP_NAME_ATTRIBUTE": "sAMAccountName",
    "LDAP_SYNC_SEARCH_BASE": "CN=Users,DC=ad,DC=seq,DC=com",
    "LDAP_DOMAIN": "ad.seq.com",
    "LDAP_URL": "ldap://10.0.3.138:389",
    "RANGER_ADMIN_PASSWORD": "admin",
    "RANGER_DB_HOST": "metastore-eu.czdydiez9kxf.eu-west-1.rds.amazonaws.com",
    "RANGER_DB_NAME": "ranger",
    "RANGER_DB_PASSWORD": "rangeradmin",
    "RANGER_DB_ROOT_PASSWORD": "Horton01",
    "RANGER_DB_ROOT_USER": "rootuser",
    "RANGER_DB_USER": "rangeradmin"
  }
}
```
If the LDAP is an AWS Directory Service then it must be in the same VPC as the cluster in order to access it.
To create the cluster:
```
hdc create-cluster --cli-input-json shared-cluster.json
```
To verify the input fields use the following command:
```
hdc describe-cluster --cluster-name krisz-shared-cluster --output json
```

### Create a connected cluster

To create a cluster which is connected to the previously created cluster generate a new skeleton:
```
hdc create-cluster generate-cli-shared-skeleton --cluster-type "Shared ephemeral" --cluster-name krisz-shared-cluster
```
Note the you have to provide an extra cluster-name parameter where you specify which cluster do you want to connect to.
This will pre-fill the fields that are required to connect to the specified cluster. It must be in the same VPC and Subnet as well.
Fill the missing fields (for the HiveMetastore only the name is required):
```
{
  "ClusterName": "krisz-connected-cluster-1",
  "HDPVersion": "2.5",
  "ClusterType": "Shared ephemeral",
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
    "InstanceCount": 2
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
    "Name": "shared-metastore"
  },
  "ClusterInputs": {
    "LDAP_DOMAIN": "ad.seq.com",
    "LDAP_URL": "ldap://10.0.3.138:389",
    "POLICYMGR_EXTERNAL_URL": "http://ip-10-0-2-11.eu-west-1.compute.internal:6080",
    "RANGER_ADMIN_PASSWORD": "admin",
    "RANGER_DB_HOST": "metastore-eu.czdydiez9kxf.eu-west-1.rds.amazonaws.com",
    "RANGER_DB_NAME": "ranger",
    "RANGER_DB_PASSWORD": "rangeradmin",
    "RANGER_DB_ROOT_PASSWORD": "Horton01",
    "RANGER_DB_ROOT_USER": "rootuser",
    "RANGER_DB_USER": "rangeradmin",
    "REMOTE_CLUSTER_NAME": "krisz-shared-cluster"
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
