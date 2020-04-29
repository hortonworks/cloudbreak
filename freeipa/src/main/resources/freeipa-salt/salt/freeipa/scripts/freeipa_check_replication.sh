#!/bin/bash

export HOSTCN=`hostname -d| sed -E 's/^|\./,dc=/g' | sed 's/^,//g'`

ldapsearch -x -h localhost -D "cn=directory manager" -w $FPW -b "$HOSTCN" -s base "(objectclass=*)"