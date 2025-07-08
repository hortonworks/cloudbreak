# Extend krb5.conf with the following content
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
