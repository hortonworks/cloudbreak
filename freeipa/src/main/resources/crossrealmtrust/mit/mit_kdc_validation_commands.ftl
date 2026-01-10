set AD_DOMAIN=${adDomain}
set IPA_DOMAIN=${ipaDomain}

REM Retrieve a Kerberos ticket for the krbtgt principal of the FreeIPA realm:
REM You should see: 'Server: krbtgt/%IPA_DOMAIN% @ %AD_DOMAIN%'
klist purge
klist get krbtgt/%IPA_DOMAIN%
klist
