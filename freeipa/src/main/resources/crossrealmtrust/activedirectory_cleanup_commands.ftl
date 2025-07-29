set AD_DOMAIN=${adDomain}
set IPA_DOMAIN=${ipaDomain}

rem Clean up of Dns setup for Active Directory cross-realm trust
dnscmd 127.0.0.1 /ZoneDelete %IPA_DOMAIN% /f

rem Remove trust from the Active Directory server towards the FreeIPA:
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /Force
