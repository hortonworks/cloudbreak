package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Service
public class KerberosTypeResolver {

    public String resolveTypeForKerberos(KerberosConfig kerberosConfig) {
        if (!Strings.isNullOrEmpty(kerberosConfig.getKerberosContainerDn()) && !Strings.isNullOrEmpty(kerberosConfig.getKerberosLdapUrl())) {
            return "active-directory";
        } else if (!Strings.isNullOrEmpty(kerberosConfig.getKerberosUrl())) {
            return "mit-kdc";
        }
        return "mit-kdc";
    }
}
