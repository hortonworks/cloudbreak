export KDC_REALM=KDC.REALM
export IPA_REALM=FREEIPA.ORG
export TRUST_SECRET="trustsecret"

kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${TRUST_SECRET}\" krbtgt/${IPA_REALM}@${KDC_REALM}"
kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${TRUST_SECRET}\" krbtgt/${KDC_REALM}@${IPA_REALM}"
