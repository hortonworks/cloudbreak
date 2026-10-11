set "AD_DOMAIN=${adDomain}"
set "IPA_DOMAIN=${ipaDomain}"
set "IPA_IPS=<#list ipaIpAddresses as ip>${ip}<#if ip_has_next> </#if></#list>"
set "TRUST_SECRET=${trustSecret}"

:: DNS setup for Active Directory cross-realm trust
:: More info: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/dnscmd
dnscmd 127.0.0.1 /zoneinfo %IPA_DOMAIN%. > NUL 2>&1 && (
  echo IPA DNS zone already exists
) || (
  echo IPA DNS zone not found, adding it...
  dnscmd 127.0.0.1 /ZoneAdd %IPA_DOMAIN%. /Forwarder %IPA_IPS% || (
    echo [FAILURE] Failed to add IPA DNS zone for %IPA_DOMAIN% domain!
  )
)

:: Set up two-way trust from the Active Directory server towards the FreeIPA
:: Remove any existing non-two-way trust to avoid LSA secret corruption when upgrading
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Remove /oneside:trusted /Force > NUL 2>&1
echo Adding two-way trust...
netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Add /Twoway /ForestTRANsitive:yes /PasswordT:%TRUST_SECRET% /oneside:trusted /verbose || (
  echo [FAILURE] Failed to add IPA trust for %IPA_DOMAIN% domain!
)
