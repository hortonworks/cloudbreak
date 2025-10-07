set AD_DOMAIN=ad.org
set IPA_DOMAIN=freeipa.org

REM Clean up of Dns setup for Active Directory cross-realm trust
dnscmd 127.0.0.1 /ZoneDelete %IPA_DOMAIN% /f

REM Remove trust from the Active Directory server towards the FreeIPA:
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /Force
