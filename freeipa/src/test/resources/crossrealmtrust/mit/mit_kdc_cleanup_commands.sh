export KDC_REALM=KDC.REALM
export IPA_REALM=FREEIPA.ORG

kadmin.local -q "delete_principal krbtgt/${IPA_REALM}@${KDC_REALM}"
kadmin.local -q "delete_principal krbtgt/${KDC_REALM}@${IPA_REALM}"
