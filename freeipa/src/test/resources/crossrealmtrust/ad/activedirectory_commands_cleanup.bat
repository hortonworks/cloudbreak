set AD_DOMAIN=ad.org
set IPA_DOMAIN=freeipa.org

REM Clean up of DNS setup for Active Directory cross-realm trust
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /ZoneDelete %IPA_DOMAIN% /f

REM Remove trust from the Active Directory server towards the FreeIPA
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netdom-trust
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /Force
