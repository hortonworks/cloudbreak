# Goal

This script helps to:
* increase the root volume of instances
* increase root volume size in launch template so that a repair does not bring back the original root volume size
  
By default most of them is 100GB, but some older DL may still have 50GB.

# Usage

## Prerequisites

* ```aws-cli```
* ```jq```
* all instances need to be in stopped state

## Invocation

 ```./increase-root-volume.sh <NEW_ROOT_VOLUME_SIZE> <INSTANCE_ID_LIST>```

* ```NEW_ROOT_VOLUME_SIZE```: The size of the new root volumes, in GB (do NOT specify the unit, just the numbers, please). It is recommended to have at least 100GB space available on the root volume
* ```INSTANCE_ID_LIST```: instance ids, separated by space, e.g.: ```instance1 instance2``` (please see how to get instance ids in the next section) 


The script only modifies settings if the size of root volumes is smaller than the requested value.

Dry run: please modify the script and set the DRY_RUN variable to true. In this way nothing will be changed.

### Get all VMs from cluster
You will need to get the VM names of a DH or DL. You can do that with cdp-cli as shown below:

#### Datalake


```
cdp datalake describe-datalake \
    --datalake-name <YOUR_DATALAKE_NAME> > describe-datalake-result.json
cat describe-datalake-result.json | jq -r '.datalake.instanceGroups[] | .instances[] | .id' | tr '\n' ' ' 
```

#### Datahub

```
cdp datahub describe-cluster  \
--cluster-name <YOUR_CLUSTER_NAME> > describe-dh-result.json

cat describe-dh-result.json | jq -r '.cluster.instanceGroups[] | .instances[] | .id' | tr '\n' ' '
```

### Example

```./increase-root-volume.sh 150 instance1 instance2 instance3```

This will increase root volume for instances instance1,2,3 and their launch templates.

## Known limitations

* there is a per volume rate limit on modifying EBS volumes. Once you hit that, you need to wait 6 hours.
* after an upgrade the original root volume size will be created => you will need to rerun this script.

## Output 

 Please save and zip following outputs and files (if they exist):

* STDOUT 
* the log file increase-root-volume.log
* output file increase-root-volume-describe-instances-result.json
* output file increase-root-volume-describe-volumes-result.json
* output file increase-root-volume-describe-autoscaling-groups-result.json

