{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}
#!/usr/bin/env bash

set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

FQDN=$(hostname -f)

getCertRequestIdFromDir() {
  CERT_DIR=$1
  NICKNAME=$2

  ipa-getcert list -d "${CERT_DIR}" -n "${NICKNAME}" | grep "Request ID" | cut -d\' -f2
}

getCertRequestIdFromFile() {
  FILE=$1

  ipa-getcert list -f "${FILE}" | grep "Request ID" | cut -d\' -f2
}

addCname() {
  NAME=$1
  CNAME=$2

  set +e
  RESULT=$(ipa dnsrecord-show "$DOMAIN" "$NAME")
  EXISTS=$?
  set -e
  if [ $EXISTS -ne 0 ]; then
    ipa dnsrecord-add "$DOMAIN" "$NAME" "--cname-rec=$CNAME"
  else
    if [[ ! ($RESULT =~ $CNAME) ]]; then
      echo "Modifying $NAME CNAME record to $CNAME"
      ipa dnsrecord-mod "$DOMAIN" "$NAME" "--cname-rec=$CNAME"
    fi
  fi
}

addHost() {
  NAME=$1

  set +e
  ipa host-show "$NAME"
  EXISTS=$?
  set -e
  if [ $EXISTS -ne 0 ]; then
    ipa host-add --force "$NAME"
  fi
}

addService() {
  SERVICE_PRINCIPAL=$1

  set +e
  ipa service-show "$SERVICE_PRINCIPAL"
  EXISTS=$?
  set -e
  if [ $EXISTS -ne 0 ]; then
    ipa service-add "$SERVICE_PRINCIPAL" --force
  fi
}

addHostToService() {
  SERVICE_PRINCIPAL=$1
  HOST=$2

  set +e
  ipa service-show "$SERVICE_PRINCIPAL" | grep "$HOST"
  EXISTS=$?
  set -e
  if [ $EXISTS -ne 0 ]; then
    ipa service-add-host "$SERVICE_PRINCIPAL" --host "$HOST"
  fi
}

checkDomainsForCert() {
  CERT_REQUEST_ID=$1
  DOMAINS_TO_ADD=$2

  DNS_LIST=$(ipa-getcert list -i "$CERT_REQUEST_ID" | grep "dns:")
  ALL_DOMAINS_EXIST=1
  for D in $DOMAINS_TO_ADD; do
    set +e
    if [[ ! $DNS_LIST == *$D* ]]; then
      ALL_DOMAINS_EXIST=0
    fi
    set -e
  done
  echo $ALL_DOMAINS_EXIST
}

checkCertForErrors() {
  CERT_REQUEST_ID=$1

  set +e
  ERROR=$(ipa-getcert list -i "$CERT_REQUEST_ID" | grep "error:")
  set -e
  if [ -n "$ERROR" ]; then
    echo "$ERROR"
    false
  fi
}

setDomainsForCert() {
  CERT_REQUEST_ID=$1
  DOMAINS_TO_ADD=$2

  EXISTS=$(checkDomainsForCert "$CERT_REQUEST_ID" "$DOMAINS_TO_ADD")
  if [ "$EXISTS" -eq 0 ]; then
    # shellcheck disable=SC2086
    ipa-getcert resubmit -D ${DOMAINS_TO_ADD// / -D } -i "$CERT_REQUEST_ID" -w
  fi

  checkCertForErrors "$CERT_REQUEST_ID"
}
echo "$(date +'%Y-%m-%d %H:%M:%S') Starting CNAME update"
# Setup basic CNAME records for pointing to FreeIPA services
echo "$FPW" | kinit "$ADMIN_USER"
addCname kdc "$LOADBALANCED_ENDPOINT.$DOMAIN."
addCname kerberos "$LOADBALANCED_ENDPOINT.$DOMAIN."
addCname ldap "$LOADBALANCED_ENDPOINT.$DOMAIN."
addCname freeipa "ipa-ca.$DOMAIN."

addHost "ldap.$DOMAIN"
addService "ldap/ldap.$DOMAIN"
addHostToService "ldap/ldap.$DOMAIN" "$FQDN"
LDAP_CERT_REQUEST_ID=$(getCertRequestIdFromDir "/etc/dirsrv/slapd-${REALM//./-}" Server-Cert)
setDomainsForCert "$LDAP_CERT_REQUEST_ID" "ldap.$DOMAIN"

addHost "freeipa.$DOMAIN"
addService "HTTP/freeipa.$DOMAIN"
addHostToService "HTTP/freeipa.$DOMAIN" "$FQDN"
addHost "kdc.$DOMAIN"
addService "HTTP/kdc.$DOMAIN"
addHostToService "HTTP/kdc.$DOMAIN" "$FQDN"
addHost "kerberos.$DOMAIN"
addService "HTTP/kerberos.$DOMAIN"
addHostToService "HTTP/kerberos.$DOMAIN" "$FQDN"


{%- if os == 'RedHat' and (osMajorRelease == 8 or osMajorRelease == 9) %}
HTTP_CERT_REQUEST_ID=$(getCertRequestIdFromFile /var/lib/ipa/certs/httpd.crt)
{%- else %}
HTTP_CERT_REQUEST_ID=$(getCertRequestIdFromDir /etc/httpd/alias Server-Cert)
{%- endif %}
setDomainsForCert "$HTTP_CERT_REQUEST_ID" "freeipa.$DOMAIN kdc.$DOMAIN kerberos.$DOMAIN"

set +e
