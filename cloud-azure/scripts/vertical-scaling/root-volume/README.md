# Goal

The two scripts help to:
1. increase the root disks of virtual machines
2. increase the root partition and filesystem to occupy the maximum allotted space.

By default most of the root volumes are 100GB, but some older DL and DH may still have 50GB.

# Limitation

> After repair or upgrade, you will need to rerun the script to resize the root disk, partition and filesystem.

# Usage

## 1. Increasing the root disk size

### Prerequisites

All the needed VMs need to be in stopped (VM deallocated) state.

The following tools need to be installed:
* az-cli
* jq
* all VMs have to be in stopped state

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

### Invocation

```./increase-root-disk-1-azure.sh <NEW_ROOT_VOLUME_SIZE> <RESOURCE_GROUP> <VM_NAME_LIST>```

Parameters:
* ```NEW_ROOT_VOLUME_SIZE```: The size of the new root volumes, in GB (do NOT specify the unit, just the numbers, please). It is recommended to have at least 100GB space available on the root volume
  
* ```RESOURCE_GROUP```: the resource group where the virtual machines are
* ```VM_NAME_LIST```: names of virtual machines, separated by space, e.g.: ```vm1 vm2```

The script only modifies settings if they are found to be below the requested root volume size.

Dry run: please modify the script and set the DRY_RUN variable to true. In this way nothing will be changed.

#### Example

```./increase-root-disk-1-azure.sh 150 rg-my-resource-group vm1 vm2 vm3```

This will increase the root disk to 150 GB for virtual machines vm1, vm2 and vm3.

## 2. Increasing the root partition and root file system

### Prerequisites

* All the needed VMs need to be in running state.

**The script by default starts in DRY_RUN mode, as it has no parameters: please open the script and set variable ```DRY_RUN``` to false.**

### Invocation

This script has to be run on every node in the cluster. For that, please log in to the master node of the cluster and use salt to distribute and run the script:

1. log in to the master node and make yourself root
2. activate the salt environment with command ```source activate_salt_env```
3. copy the script ```increase-root-disk-2-azure.sh``` to the master node
4. distribute the script to all nodes: ```salt-cp '*' increase-root-disk-2-azure.sh /home/cloudbreak```
5. make the script executable on all nodes: ```salt '*' cmd.run 'chmod 744 /home/cloudbreak/increase-root-disk-2-azure.sh'```
6. run the script on every node and save output: ```salt '*' cmd.run './home/cloudbreak/increase-root-disk-2-azure.sh' > increase-root-disk-2-azure.out```
7. check the output that it succeeded on every node (you can compare it with the sample output)

It will only modify partition or file system size if it is smaller than the disk size.

### Sample output
Root disk was successfully increased from 100GB to 160GB.
Output was taken 06/08/2021.

    Increasing root volume, V1.0 - resizing partition and filesystem of OS Disk
    =======================================
    
    Increasing the root volume on azure is composed of two steps:
        1. increase the root disk itself
        2. increase the partition on the running instance
    
    This script is part 2.
    It will only increase partition or filesystem size if the current size is smaller than allowed by the disk.
    
    How to use it:
        1. log in to the master node and make yourself root
        2. activate the salt environment
        3. distribute this script to all nodes: salt-cp '*' increase-root-disk-2-azure.sh /home/cloudbreak
        4. make the script executable on all nodes: salt '*' cmd.run 'chmod 744 /home/cloudbreak/increase-root-disk-2-azure.sh'
        5. run the script on every node and save output: salt '*' cmd.run './home/cloudbreak/increase-root-disk-2-azure.sh' > increase-root-disk-2-azure.out
    
    
    root disk properties: 
        * path: /dev/sda 
        * size in bytes: 171798691840 (160G GB)
    
    root partition properties: 
        * path: /dev/sda2
        * number: 2
        * size in bytes: 106848828928 (99.5G GB)
    
    The partition /dev/sda2 size 106848828928 (99.5G GB) is much smaller than the disk size 171798691840 (160G GB), resize is needed
    executing growpart
    CHANGED: partition=2 start=1026048 old: size=208689119 end=209715167 new: size=334518239 end=335544287
    partition
    
    root file system size is 104329448 KB (100 GB)
    
    file system needs resizing
    executing xfs_growfs
    meta-data=/dev/sda2              isize=512    agcount=14, agsize=1934016 blks
             =                       sectsz=512   attr=2, projid32bit=1
             =                       crc=1        finobt=0 spinodes=0
    data     =                       bsize=4096   blocks=26086139, imaxpct=25
             =                       sunit=0      swidth=0 blks
    naming   =version 2              bsize=4096   ascii-ci=0 ftype=1
    log      =internal               bsize=4096   blocks=3777, version=2
             =                       sectsz=512   sunit=0 blks, lazy-count=1
    realtime =none                   extsz=4096   blocks=0, rtextents=0
    data blocks changed from 26086139 to 41814779
    Finished
