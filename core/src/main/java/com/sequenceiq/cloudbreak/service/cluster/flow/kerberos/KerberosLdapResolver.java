package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Service
public class KerberosLdapResolver {

    public String resolveLdapUrlForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosLdapUrl()) ? null : kerberosConfig.getKerberosLdapUrl();
    }
}
