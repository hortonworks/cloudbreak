# Remove the following content from krb5.conf

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
