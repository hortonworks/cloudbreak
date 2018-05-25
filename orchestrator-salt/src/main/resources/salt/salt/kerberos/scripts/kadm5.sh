#!/bin/bash

test -f /var/kerberos/krb5kdc/kadm5.acl || touch /var/kerberos/krb5kdc/kadm5.acl

if grep "*/admin@{{ realm }} *" /var/kerberos/krb5kdc/kadm5.acl; then echo ok; else echo "*/admin@{{ realm }} *" >> /var/kerberos/krb5kdc/kadm5.acl; fi

for host in {{ kdcs }}; do
  if grep "kiprop/$host@{{ realm }} *" /var/kerberos/krb5kdc/kadm5.acl; then echo ok; else echo "kiprop/$host@{{ realm }} *" >> /var/kerberos/krb5kdc/kadm5.acl; fi
done