-- // CLOUD-45782 storing images in versioned component table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE component_table
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE component
(
   id              bigint PRIMARY KEY NOT NULL DEFAULT nextval('component_table'),
   componenttype   varchar (63) NOT NULL,
   name            varchar (255) NOT NULL,
   stack_id        bigint NOT NULL,
   attributes      text NOT NULL
);

ALTER TABLE component
   ADD CONSTRAINT fk_component_stack FOREIGN KEY (stack_id)
       REFERENCES stack (id);


ALTER TABLE component
   ADD CONSTRAINT uk_component_componenttype_name_stack UNIQUE
          (componenttype, name, stack_id);


INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'IMAGE' AS componentType,
          'image' AS NAME,
          s.id AS stack_id,
             '{"imageName":"'
          || s.image
          || '","userdata":{"CORE":"#!/bin/bash\nset -x\n\nSTART_LABEL=97\nPLATFORM_DISK_PREFIX=xvd\n\n\nget_ip() {\n  ifconfig eth0 | awk ''/inet addr/{print substr($2,6)}''\n}\n\nfix_hostname() {\n  if grep -q $(get_ip) /etc/hosts ;then\n    sed -i \"/$(get_ip)/d\" /etc/hosts\n  else\n    echo OK\n  fi\n}\n\nextend_rootfs() {\n  # Usable on GCP, does not harm anywhere else\n  root_fs_device=$(mount | grep '' / '' | cut -d'' '' -f 1 | sed s/1//g)\n  growpart $root_fs_device 1\n  xfs_growfs /\n}\n\nformat_disks() {\n  mkdir /hadoopfs\n  for (( i=1; i<=24; i++ )); do\n    LABEL=$(printf \"\\x$(printf %x $((START_LABEL+i)))\")\n    DEVICE=/dev/${PLATFORM_DISK_PREFIX}${LABEL}\n    if [ -e $DEVICE ]; then\n      MOUNTPOINT=$(grep $DEVICE /etc/fstab | tr -s '' \\t'' '' '' | cut -d'' '' -f 2)\n      if [ -n \"$MOUNTPOINT\" ]; then\n        umount \"$MOUNTPOINT\"\n        sed -i \"\\|^$DEVICE|d\" /etc/fstab\n      fi\n      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE\n      mkdir /hadoopfs/fs${i}\n      echo $DEVICE /hadoopfs/fs${i} ext4  defaults,noatime 0 2 >> /etc/fstab\n      mount /hadoopfs/fs${i}\n    fi\n  done\n  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch\n}\n\nreload_sysconf() {\n  sysctl -p\n}\n\nmain() {\n  reload_sysconf\n  if [[ \"$1\" == \"::\" ]]; then\n    shift\n    eval \"$@\"\n  elif [ ! -f \"/var/cb-init-executed\" ]; then\n    extend_rootfs\n    format_disks\n    fix_hostname\n    touch /var/cb-init-executed\n    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed\n  fi\n}\n\n[[ \"$0\" == \"$BASH_SOURCE\" ]] && main \"$@\"","GATEWAY":"#!/bin/bash\nset -x\n\nSTART_LABEL=97\nPLATFORM_DISK_PREFIX=xvd\n\nsetup_tmp_ssh() {\n  echo \"#tmpssh_start\" >> /home/ec2-user/.ssh/authorized_keys\n  echo \"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCEueQQkCxiuRLcsCL8mOAKq9ddsyQLlPQt5Ue1LDUQ2hAqF1rXznXCkKBD1D351D+D4FRwK1jjq/+qeRltDcl4IsWndSBCQD30arZ8uaMSThvL9EVc11f5p4tYYP8y/81Xg/Kfnq8E31Czq0ljCXhvPmMMUuowTOooPUoyo0M/H4r60/o7KqrPWrxQpvIEzicfo9RUvulm6sBv1U09fUomgEnsMQAeVEYdVNnimF0O/WUzqYAsydtjB7xJ/vnSk3TgihNjPdN4ee5SCiZfPxqFayqp+ENgZMA/KuP/79zYMolhlqIm6J2+mJr7T7miYP/zTLgisypsE6BxvnpGJJcn cloudbreak\n\" >> /home/ec2-user/.ssh/authorized_keys\n  echo \"#tmpssh_end\" >> /home/ec2-user/.ssh/authorized_keys\n}\n\nget_ip() {\n  ifconfig eth0 | awk ''/inet addr/{print substr($2,6)}''\n}\n\nfix_hostname() {\n  if grep -q $(get_ip) /etc/hosts ;then\n    sed -i \"/$(get_ip)/d\" /etc/hosts\n  else\n    echo OK\n  fi\n}\n\nextend_rootfs() {\n  # Usable on GCP, does not harm anywhere else\n  root_fs_device=$(mount | grep '' / '' | cut -d'' '' -f 1 | sed s/1//g)\n  growpart $root_fs_device 1\n  xfs_growfs /\n}\n\nformat_disks() {\n  mkdir /hadoopfs\n  for (( i=1; i<=24; i++ )); do\n    LABEL=$(printf \"\\x$(printf %x $((START_LABEL+i)))\")\n    DEVICE=/dev/${PLATFORM_DISK_PREFIX}${LABEL}\n    if [ -e $DEVICE ]; then\n      MOUNTPOINT=$(grep $DEVICE /etc/fstab | tr -s '' \\t'' '' '' | cut -d'' '' -f 2)\n      if [ -n \"$MOUNTPOINT\" ]; then\n        umount \"$MOUNTPOINT\"\n        sed -i \"\\|^$DEVICE|d\" /etc/fstab\n      fi\n      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE\n      mkdir /hadoopfs/fs${i}\n      echo $DEVICE /hadoopfs/fs${i} ext4  defaults,noatime 0 2 >> /etc/fstab\n      mount /hadoopfs/fs${i}\n    fi\n  done\n  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch\n}\n\nreload_sysconf() {\n  sysctl -p\n}\n\nmain() {\n  reload_sysconf\n  if [[ \"$1\" == \"::\" ]]; then\n    shift\n    eval \"$@\"\n  elif [ ! -f \"/var/cb-init-executed\" ]; then\n    setup_tmp_ssh\n    extend_rootfs\n    format_disks\n    fix_hostname\n    touch /var/cb-init-executed\n    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed\n  fi\n}\n\n[[ \"$0\" == \"$BASH_SOURCE\" ]] && main \"$@\""}}'
             AS attributes
     FROM stack s JOIN credential c ON c.id = s.credential_id WHERE c.dtype = 'AwsCredential';



INSERT INTO component (componenttype,
                       name,
                       stack_id,
                       attributes)
   SELECT 'IMAGE' AS componentType,
          'image' AS NAME,
          s.id AS stack_id,
             '{"imageName":"'
          || s.image
          || '","userdata":{"CORE":"#!/bin/bash\nset -x\n\nSTART_LABEL=97\nPLATFORM_DISK_PREFIX=sd\n\n\nget_ip() {\n  ifconfig eth0 | awk ''/inet addr/{print substr($2,6)}''\n}\n\nfix_hostname() {\n  if grep -q $(get_ip) /etc/hosts ;then\n    sed -i \"/$(get_ip)/d\" /etc/hosts\n  else\n    echo OK\n  fi\n}\n\nextend_rootfs() {\n  # Usable on GCP, does not harm anywhere else\n  root_fs_device=$(mount | grep '' / '' | cut -d'' '' -f 1 | sed s/1//g)\n  growpart $root_fs_device 1\n  xfs_growfs /\n}\n\nformat_disks() {\n  mkdir /hadoopfs\n  for (( i=1; i<=24; i++ )); do\n    LABEL=$(printf \"\\x$(printf %x $((START_LABEL+i)))\")\n    DEVICE=/dev/${PLATFORM_DISK_PREFIX}${LABEL}\n    if [ -e $DEVICE ]; then\n      MOUNTPOINT=$(grep $DEVICE /etc/fstab | tr -s '' \\t'' '' '' | cut -d'' '' -f 2)\n      if [ -n \"$MOUNTPOINT\" ]; then\n        umount \"$MOUNTPOINT\"\n        sed -i \"\\|^$DEVICE|d\" /etc/fstab\n      fi\n      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE\n      mkdir /hadoopfs/fs${i}\n      echo $DEVICE /hadoopfs/fs${i} ext4  defaults,noatime 0 2 >> /etc/fstab\n      mount /hadoopfs/fs${i}\n    fi\n  done\n  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch\n}\n\nreload_sysconf() {\n  sysctl -p\n}\n\nmain() {\n  reload_sysconf\n  if [[ \"$1\" == \"::\" ]]; then\n    shift\n    eval \"$@\"\n  elif [ ! -f \"/var/cb-init-executed\" ]; then\n    extend_rootfs\n    format_disks\n    fix_hostname\n    touch /var/cb-init-executed\n    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed\n  fi\n}\n\n[[ \"$0\" == \"$BASH_SOURCE\" ]] && main \"$@\"","GATEWAY":"#!/bin/bash\nset -x\n\nSTART_LABEL=97\nPLATFORM_DISK_PREFIX=sd\n\nsetup_tmp_ssh() {\n  echo \"#tmpssh_start\" >> /home/cloudbreak/.ssh/authorized_keys\n  echo \"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDVNqwuorkfB0Wd1lprco/16qdWyyrj4RhEjqXmi3oDzjQ92oFkNwNB2/NsYnsRNh/fnABBhxWikZmEIgNvAKhbQ4GdQWEjucBymmOm6tXN0of0I1p69xZaiTrumz2KwfdRb3p0WUjo0b8+GOIA2OYX2G/kTnG2t6mHEqEaHQDxRC/KLJEVxZwhj3g1yqtn2mrDpHojjDyeDN6oHuBf4aY2ug08BRS43d69IGfxgJcX4DmZGxXE7QeJKV1+x9B1OMafiCoRYhSHw++HKvgYvoq/yGCusIkf7YeiGcn25nzlx8f/WvYRoPWb+xEPC6Cuq06mGpjs8QdRhsyUwcld3k8f cloudbreak\n\" >> /home/cloudbreak/.ssh/authorized_keys\n  echo \"#tmpssh_end\" >> /home/cloudbreak/.ssh/authorized_keys\n}\n\nget_ip() {\n  ifconfig eth0 | awk ''/inet addr/{print substr($2,6)}''\n}\n\nfix_hostname() {\n  if grep -q $(get_ip) /etc/hosts ;then\n    sed -i \"/$(get_ip)/d\" /etc/hosts\n  else\n    echo OK\n  fi\n}\n\nextend_rootfs() {\n  # Usable on GCP, does not harm anywhere else\n  root_fs_device=$(mount | grep '' / '' | cut -d'' '' -f 1 | sed s/1//g)\n  growpart $root_fs_device 1\n  xfs_growfs /\n}\n\nformat_disks() {\n  mkdir /hadoopfs\n  for (( i=1; i<=24; i++ )); do\n    LABEL=$(printf \"\\x$(printf %x $((START_LABEL+i)))\")\n    DEVICE=/dev/${PLATFORM_DISK_PREFIX}${LABEL}\n    if [ -e $DEVICE ]; then\n      MOUNTPOINT=$(grep $DEVICE /etc/fstab | tr -s '' \\t'' '' '' | cut -d'' '' -f 2)\n      if [ -n \"$MOUNTPOINT\" ]; then\n        umount \"$MOUNTPOINT\"\n        sed -i \"\\|^$DEVICE|d\" /etc/fstab\n      fi\n      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE\n      mkdir /hadoopfs/fs${i}\n      echo $DEVICE /hadoopfs/fs${i} ext4  defaults,noatime 0 2 >> /etc/fstab\n      mount /hadoopfs/fs${i}\n    fi\n  done\n  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch\n}\n\nreload_sysconf() {\n  sysctl -p\n}\n\nmain() {\n  reload_sysconf\n  if [[ \"$1\" == \"::\" ]]; then\n    shift\n    eval \"$@\"\n  elif [ ! -f \"/var/cb-init-executed\" ]; then\n    setup_tmp_ssh\n    extend_rootfs\n    format_disks\n    fix_hostname\n    touch /var/cb-init-executed\n    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed\n  fi\n}\n\n[[ \"$0\" == \"$BASH_SOURCE\" ]] && main \"$@\""}}'
             AS attributes
     FROM stack s JOIN credential c ON c.id = s.credential_id WHERE c.dtype <> 'AwsCredential';


ALTER TABLE stack
   DROP COLUMN image;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ADD COLUMN image varchar (255);


ALTER TABLE component ALTER COLUMN attributes TYPE JSON USING attributes::JSON;

UPDATE
    stack
SET
    image = c.attributes->>'imageName'
FROM
    stack s
INNER JOIN
    component c
ON
    s.id = c.stack_id;


DROP TABLE component;

DROP SEQUENCE component_table;
