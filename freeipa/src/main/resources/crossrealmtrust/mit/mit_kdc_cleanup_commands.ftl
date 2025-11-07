export KDC_REALM=${kdcRealm}
export IPA_REALM=${ipaRealm}

kadmin.local -q "delete_principal krbtgt/${"$"}{IPA_REALM}@${"$"}{KDC_REALM}"
kadmin.local -q "delete_principal krbtgt/${"$"}{KDC_REALM}@${"$"}{IPA_REALM}"
