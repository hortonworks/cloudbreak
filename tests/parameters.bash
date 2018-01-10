#!/bin/bash

: ${BLUEPRINT_URL:=https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp}
: ${BLUEPRINT_NAME:="EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0"}
: ${CLUSTER_NAME:="openstack-cluster"}
: ${AZURE_CREDENTIAL_NAME:="azure"}
: ${AWS_CREDENTIAL_NAME:="amazon"}
: ${GCP_CREDENTIAL_NAME:="google"}
: ${OPENSTACK_CREDENTIAL_NAME:="openstack"}

AWS_ARGS_KEY=" --name cli-aws-key --access-key testaccess --secret-key testsecretkey "
AWS_ARGS_ROLE=" --name cli-aws-role --role-arn  testawsrole "
OPENSTACK_ARGS_V2=" --name $OPENSTACK_CREDENTIAL_NAME --tenant-user cloudbreak  --tenant-password cloudbreak --tenant-name cloudbreak --endpoint http://openstack.eng.com:3000/v2.0"
OPENSTACK_ARGS_V3=" --name $OPENSTACK_CREDENTIAL_NAME --tenant-user cloudbreak  --tenant-password cloudbreak --user-domain cloudbreak --endpoint http://openstack.eng.com:3000/v3.0"
GCP_ARGS=" --name $GCP_CREDENTIAL_NAME --project-id cloudbreak --service-account-id 1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com --service-account-private-key-file test.p12"
AZURE_ARGS=" --name $AZURE_CREDENTIAL_NAME --subscription-id a12b1234-1234-12aa-3bcc-4d5e6f78900g --tenant-id a12b1234-1234-12aa-3bcc-4d5e6f78900g --app-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --app-password cloudbreak"

COMMON_ARGS_WO_CLUSTER=" --server ${CLOUD_URL} --username ${EMAIL} --password ${PASSWORD}  "
