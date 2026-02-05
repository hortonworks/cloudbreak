set AD_DOMAIN=ad.org
set IPA_DOMAIN=freeipa.org
set IPA_IPS=ipaIp1 ipaIp2 ipaIp3
set TRUST_SECRET=trustSecret

:: DNS setup for Active Directory cross-realm trust
:: More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /zoneinfo %IPA_DOMAIN%. > NUL 2>&1 || (
  echo IPA DNS zone not found, adding it...
  dnscmd 127.0.0.1 /ZoneAdd %IPA_DOMAIN%. /Forwarder %IPA_IPS% || (
    echo [FAILURE] Failed to add IPA DNS zone for %IPA_DOMAIN% domain!
  )
)

:: Set up trust from the Active Directory server towards the FreeIPA
:: More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netdom-trust
netdom query /Domain:%AD_DOMAIN% /Direct TRUST | findstr %IPA_DOMAIN% > NUL 2>&1 || (
  echo IPA trust not found, adding it...
  netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Add /Twoway /ForestTRANsitive:yes /PasswordT:%TRUST_SECRET% /oneside:trusted || (
    echo [FAILURE] Failed to add IPA trust for %IPA_DOMAIN% domain!
  )
)
