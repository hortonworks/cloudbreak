package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import org.springframework.stereotype.Service;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Service
public class KerberosPrincipalResolver {

    private static final String PRINCIPAL = "/admin";

    public String resolvePrincipalForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosPrincipal()) ? kerberosConfig.getKerberosAdmin() + PRINCIPAL
                : kerberosConfig.getKerberosPrincipal();
    }
}
