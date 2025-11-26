# Create a new file called cdp_resourceName_krb5.conf under /etc/krb5.conf.d by executing the following commands
cat > /etc/krb5.conf.d/cdp_resourceName_krb5.conf <<EOF
[domain_realm]
.freeipa.org = FREEIPA.ORG
freeipa.org = FREEIPA.ORG

[capaths]
AD.ORG = {
  FREEIPA.ORG = AD.ORG
}

FREEIPA.ORG = {
  AD.ORG = AD.ORG
}
EOF
chmod 644 /etc/krb5.conf.d/cdp_resourceName_krb5.conf
