export KDC_REALM=${kdcRealm}
export IPA_REALM=${ipaRealm}
export TRUST_SECRET="${trustSecret}"

kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${"$"}{TRUST_SECRET}\" krbtgt/${"$"}{IPA_REALM}@${"$"}{KDC_REALM}"
kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${"$"}{TRUST_SECRET}\" krbtgt/${"$"}{KDC_REALM}@${"$"}{IPA_REALM}"
