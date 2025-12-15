set AD_DOMAIN=ad.org
set IPA_DOMAIN=freeipa.org
set IPA_IPS=ipaIp1 ipaIp2 ipaIp3
set TRUST_SECRET=trustSecret

REM DNS setup for Active Directory cross-realm trust
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /ZoneAdd %IPA_DOMAIN%. /Forwarder %IPA_IPS%

REM Set up trust from the Active Directory server towards the FreeIPA
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netdom-trust
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Add /Twoway /ForestTRANsitive:yes /PasswordT:%TRUST_SECRET% /oneside:trusted
