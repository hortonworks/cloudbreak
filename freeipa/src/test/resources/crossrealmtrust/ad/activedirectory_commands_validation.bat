set AD_DOMAIN=ad.org
set IPA_DOMAIN=freeipa.org

REM Secure channel validation from the Active Directory server towards the FreeIPA:
nltest /sc_verify:%IPA_DOMAIN%

REM Retrieve a Kerberos ticket for the krbtgt principal of the FreeIPA realm:
REM You should see: 'Server: krbtgt/freeipa.org @ ad.org'
klist purge
klist get krbtgt/%IPA_DOMAIN%
klist
