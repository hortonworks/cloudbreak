#!/bin/sh

mkdir -p /hadoopfs

MOUNTPATH_ID=$(lsblk -o MOUNTPOINT -n | grep '/hadoopfs' | tail -1 | sed -n "s/\/hadoopfs\/fs\(\d\)*/\1/p")
for (( i=0; i<=26; i++ )); do
    DEVICE=/dev/nvme${i}n1
    if [ -e $DEVICE ]; then
        MOUNTPOINT=$(lsblk $DEVICE -o MOUNTPOINT -n)
        if [ -z "$MOUNTPOINT" ]; then
            MOUNTPATH_ID=$((MOUNTPATH_ID+1))
            mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE
            mkdir /hadoopfs/fs${MOUNTPATH_ID}
            echo UUID=$(blkid -o value $DEVICE | head -1) /hadoopfs/fs${MOUNTPATH_ID} ext4  defaults,noatime,nofail 0 2 >> /etc/fstab
            mount /hadoopfs/fs${MOUNTPATH_ID}
            chmod 777 /hadoopfs/fs${MOUNTPATH_ID}
        fi
    fi
done
