set AD_DOMAIN=${adDomain}
set IPA_DOMAIN=${ipaDomain}

:: Clean up of DNS setup and its caches for Active Directory cross-realm trust
:: More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /zoneinfo %IPA_DOMAIN%. > NUL 2>&1 && (
  echo IPA zone found, removing it and its caches...
  dnscmd 127.0.0.1 /ZoneDelete %IPA_DOMAIN% /f || (
    echo [FAILURE] Failed to remove IPA DNS zone for %IPA_DOMAIN% domain!
  )
  dnscmd 127.0.0.1 /ClearCache > NUL 2>&1
  :: More info: https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2012-R2-and-2012/cc731935(v=ws.11)
  nltest /dsgetdc:%IPA_DOMAIN% /force > NUL 2>&1 || echo:
)

:: Remove trust from the Active Directory server towards the FreeIPA
:: More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netdom-trust
netdom query /Domain:%AD_DOMAIN% /Direct TRUST | findstr %IPA_DOMAIN% > NUL 2>&1 && (
  echo IPA trust found, removing it...
  netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /Force || (
    echo [FAILURE] Failed to remove IPA trust for %IPA_DOMAIN% domain!
  )
)
