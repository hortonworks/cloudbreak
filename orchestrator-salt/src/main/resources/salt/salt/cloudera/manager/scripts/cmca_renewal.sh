{%- set cm_keytab_file = salt['pillar.get']('keytab:CM:path') %}
{%- set cm_principal = salt['pillar.get']('keytab:CM:principal') %}
{%- set manager_server_hostname = salt['grains.get']('host') %}
{%- set manager_server_fqdn = salt['grains.get']('fqdn') %}
{%- set internal_loadbalancer_san = salt['pillar.get']('cloudera-manager:communication:internal_loadbalancer_san') %}
{%- set gov_cloud = salt['pillar.get']('cluster:gov_cloud', False) %}
#!/usr/bin/env bash

HOSTNAME={{ manager_server_hostname }}
FQDN={{ manager_server_fqdn }}
CACERTS_DIR="/opt/cacerts"
CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
CERTMANAGER_ARGS=
LOADBALANCER_SAN={{ internal_loadbalancer_san }}
CM_KEYTAB_FILE={{ cm_keytab_file }}
CM_PRINCIPAL={{ cm_principal }}
OUT_FILE=$(mktemp -t signed_ca_chain.XXXXXX.pem)
ALTNAME=" --altname DNS:${FQDN} "
if [ -n "$LOADBALANCER_SAN" ]; then
  ALTNAME+="--altname ${LOADBALANCER_SAN} "
fi
OVERRIDES="--override ca_dn=CN=${HOSTNAME} "
{% if gov_cloud == True %}
  OVERRIDES+="--override keystore_type=BCFKS "
{% endif %}

echo "$(date '+%d/%m/%Y %H:%M:%S') - Generating new CMCA."
mv ${CERTMANAGER_DIR} ${CERTMANAGER_DIR}_bkp_$(date '+%d%m%Y%H%M%S')
/opt/cloudera/cm-agent/bin/certmanager --location ${CERTMANAGER_DIR} setup --skip-invalid-ca-certs --configure-services ${CERTMANAGER_ARGS} ${OVERRIDES} --stop-at-csr ${ALTNAME} --trusted-ca-certs ${CACERTS_DIR}/cacerts.pem
kinit -kt ${CM_KEYTAB_FILE} ${CM_PRINCIPAL}
/opt/cloudera/cm/bin/generate_intermediate_ca_ipa.sh ${CM_PRINCIPAL} ${CERTMANAGER_DIR}/CMCA/private/ca_csr.pem ${OUT_FILE}
/opt/cloudera/cm-agent/bin/certmanager --location ${CERTMANAGER_DIR} setup --skip-invalid-ca-certs --configure-services ${CERTMANAGER_ARGS} ${OVERRIDES} --signed-ca-cert=${OUT_FILE} --skip-cm-init ${ALTNAME} --trusted-ca-certs ${CACERTS_DIR}/cacerts.pem > ${CERTMANAGER_DIR}/auto-tls.init.txt

echo "$(date '+%d/%m/%Y %H:%M:%S') - Updating cm.settings."
AUTO_TLS_INIT_FILE=${CERTMANAGER_DIR}/auto-tls.init.txt
CM_SETTINGS_FILE=/etc/cloudera-scm-server/cm.settings
cp ${CM_SETTINGS_FILE} ${CM_SETTINGS_FILE}.bkp.$(date '+%d%m%Y%H%M%S')
while IFS= read -r line
do
  stringarray=($line)
{% raw %}
  if [ ${#stringarray[@]} -eq 3 ]; then
{% endraw %}
    variable=${stringarray[1]}
    newvalue=${stringarray[2]}
    if ! grep -q "setsettings ${variable} ${newvalue}" ${CM_SETTINGS_FILE}; then
      echo "updating value of ${variable} in cm.settings"
      sed -i 's/^.*setsettings '"$variable"' .*$/setsettings '"$variable"' '"$newvalue"'/' ${CM_SETTINGS_FILE}
    fi
  fi
done < "$AUTO_TLS_INIT_FILE"

echo "$(date '+%d/%m/%Y %H:%M:%S') - Cleaning up leftover after CMCA renewal."
rm -rf ${CERTMANAGER_DIR}_bkp_*
rm -f ${CM_SETTINGS_FILE}.bkp.*
