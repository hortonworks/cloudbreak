rem Dns setup for Active Directory cross-realm trust

set AD_DOMAIN=ad.org
set IPA_IPS=ipaIp1 ipaIp2 ipaIp3
set IPA_DOMAIN=freeipa.org
set TRUST_SECRET=trustSecret

dnscmd 127.0.0.1 /ZoneAdd %IPA_DOMAIN%. /Forwarder %IPA_IPS%

rem Set up trust from the Active Directory server towards the FreeIPA:

netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Add /Twoway /ForestTRANsitive:yes /PasswordT:%TRUST_SECRET% /oneside:trusted
