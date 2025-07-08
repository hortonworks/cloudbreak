rem Dns setup for Active Directory cross-realm trust

set AD_DOMAIN=${adDomain}
set IPA_IPS=<#list ipaIpAdresses as ip>${ip}<#if ip_has_next> </#if></#list>
set IPA_DOMAIN=${ipaDomain}
set TRUST_SECRET=${trustSecret}

dnscmd 127.0.0.1 /ZoneAdd %IPA_DOMAIN%. /Forwarder %IPA_IPS%

rem Set up trust from the Active Directory server towards the FreeIPA:

netdom trust %IPA_DOMAIN% /Domain:%AD_DOMAIN% /Add /Twoway /ForestTRANsitive:yes /PasswordT:%TRUST_SECRET% /oneside:trusted
