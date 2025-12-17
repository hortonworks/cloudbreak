set AD_DOMAIN=${adDomain}
set IPA_DOMAIN=${ipaDomain}

REM Clean up of DNS setup and its caches for Active Directory cross-realm trust
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /ZoneDelete %IPA_DOMAIN% /f
dnscmd 127.0.0.1 /ClearCache
REM More info: https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2012-R2-and-2012/cc731935(v=ws.11)
nltest /dsgetdc:%IPA_DOMAIN% /force

REM Remove trust from the Active Directory server towards the FreeIPA
REM More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netdom-trust
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /Force
