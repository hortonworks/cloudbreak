export KDC_REALM=${kdcRealm}
export IPA_REALM=${ipaRealm}

# Remove trust from the MIT KDC Server towards the FreeIPA
# More info: https://web.mit.edu/kerberos/krb5-latest/doc/admin/admin_commands/kadmin_local.html
kadmin.local -q "delete_principal krbtgt/${"$"}{IPA_REALM}@${"$"}{KDC_REALM}"
kadmin.local -q "delete_principal krbtgt/${"$"}{KDC_REALM}@${"$"}{IPA_REALM}"
