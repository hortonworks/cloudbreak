set AD_DOMAIN=${adDomain}
set IPA_DOMAIN=${ipaDomain}

REM Secure channel validation from the Active Directory server towards the FreeIPA:
nltest /sc_verify:%IPA_DOMAIN%

REM Retrieve a Kerberos ticket for the krbtgt principal of the FreeIPA realm:
REM You should see: 'Server: krbtgt/${ipaDomain} @ ${adDomain}'
klist purge
klist get krbtgt/%IPA_DOMAIN%
klist
