package cli

var AWSCreateClusterSkeletonHelp = `
{
  "ClusterName": "my-cluster",                            // Name of the cluster
  "HDPVersion": "2.5",                                    // HDP version
  "ClusterType": "EDW-ETL: Apache Spark 2.0-preview",     // Cluster type
  "Master": {                                             // Master instance group
    "InstanceType": "m4.xlarge",                          // Instance type of master instance group
    "VolumeType": "gp2",                                  // Volume type of master instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 32,                                     // Volume size of master instace group
    "VolumeCount": 1                                      // Volume count of master instance group
  },
  "Worker": {                                             // Worker instance group
    "InstanceType": "m3.xlarge",                          // Instance type of worker instance group
    "VolumeType": "ephemeral",                            // Volume type of worker instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 40,                                     // Volume size of worker instace group
    "VolumeCount": 2,                                     // Volume count of master instance group
    "InstanceCount": 1                                    // Instance count of workerk instance group, accepted value: >1
  },
  "SSHKeyName": "my-existing-keypair-name",               // Name of an existing EC2 KeyPair to enable SSH access to the cluster node instances.
  "RemoteAccess": "0.0.0.0/0",                            // Allow connections from this address range. Must be a valid CIDR IP (for example: 0.0.0.0/0 will allow access from all).
  "WebAccess": true,                                      // Open access to web UI (Ambari, Spark, Zeppelin)
  "ClusterAndAmbariUser": "admin",                        // User name for Ambari and all services
  "ClusterAndAmbariPassword": "admin",                    // Password for Ambari and all services
  "InstanceRole": "CREATE"                                // Instance role to access S3, accepted values: "", null, CREATE, existing AWS instance role name
}
`
