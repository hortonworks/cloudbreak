package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Service
public class KerberosRealmResolver {

    public String getRealm(String gwDomain, KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosRealm()) ? gwDomain.toUpperCase() : kerberosConfig.getKerberosRealm().toUpperCase();
    }
}
