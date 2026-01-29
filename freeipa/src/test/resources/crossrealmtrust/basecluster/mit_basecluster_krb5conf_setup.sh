# Create a new file called cdp_resourceName_krb5.conf under /etc/krb5.conf.d with the following content
# Make sure the line "includedir /etc/krb5.conf.d/" is included by default at the top of /etc/krb5.conf

cat > /etc/krb5.conf.d/cdp_resourceName_krb5.conf <<EOF
[realms]
FREEIPA.ORG = {
  kdc = ipa.freeipa.org
  admin_server = ipa.freeipa.org
}

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
