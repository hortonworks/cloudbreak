#!/bin/bash

for host in {{ kdcs }}; do
  if grep -w "host/$host@{{ realm }}" /var/kerberos/krb5kdc/kpropd.acl; then echo ok; else echo "host/$host@{{ realm }}" >> /var/kerberos/krb5kdc/kpropd.acl; fi
done