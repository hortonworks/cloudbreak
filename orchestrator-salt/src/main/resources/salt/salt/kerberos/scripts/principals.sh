#!/bin/bash

# admin principal
if kadmin.local -q "list_principals *" | grep -w "{{ usr }}/admin@{{ realm }}"; then echo ok; else kadmin.local -q "addprinc -pw {{ pw }} {{ usr }}/admin"; fi

# host/kiprop principals for master-slave authentication and database propagation
for host in {{ kdcs }}; do
  if kadmin.local -q "list_principals *" | grep -w "host/$host@{{ realm }}"; then echo ok; else kadmin.local -q "addprinc -pw {{ pw }} host/$host"; fi
  if kadmin.local -q "list_principals *" | grep -w "kiprop/$host@{{ realm }}"; then echo ok; else kadmin.local -q "addprinc -pw {{ pw }} kiprop/$host"; fi
done

# default keytab with host and kiprop principals
if test -f /etc/krb5.keytab;
then
  echo ok;
else
  for host in {{ kdcs }}; do
    kadmin.local -q "ktadd host/$host"
    kadmin.local -q "ktadd kiprop/$host"
  done
fi

salt-cp -G 'roles:kerberos_server_slave' /etc/krb5.keytab /etc/krb5.keytab
salt-cp -G 'roles:kerberos_server_slave' /var/kerberos/krb5kdc/.k5.{{ realm }} /var/kerberos/krb5kdc/.k5.{{ realm }}
