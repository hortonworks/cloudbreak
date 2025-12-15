export KDC_REALM=${kdcRealm}
export IPA_REALM=${ipaRealm}
export TRUST_SECRET="${trustSecret}"

# Set up trust from the MIT KDC Server towards the FreeIPA:
# More info: https://web.mit.edu/kerberos/krb5-latest/doc/admin/admin_commands/kadmin_local.html
kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${"$"}{TRUST_SECRET}\" krbtgt/${"$"}{IPA_REALM}@${"$"}{KDC_REALM}"
kadmin.local -q "add_principal -requires_preauth -e aes256-cts,aes128-cts -pw \"${"$"}{TRUST_SECRET}\" krbtgt/${"$"}{KDC_REALM}@${"$"}{IPA_REALM}"
