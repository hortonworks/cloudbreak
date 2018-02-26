package com.sequenceiq.cloudbreak.blueprint.kerberos;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    private final Gson gson = new Gson();

    public String resolveTypeForKerberos(KerberosConfig kerberosConfig) {
        return kerberosConfig.getType() == KerberosType.EXISTING_AD ? "active-directory" : "mit-kdc";
    }

    public String resolveHostForKerberos(Cluster cluster, String defaultHost) {
        return Strings.isNullOrEmpty(cluster.getKerberosConfig().getUrl()) ? defaultHost : cluster.getKerberosConfig().getUrl();
    }

    public String resolveHostForKdcAdmin(Cluster cluster, String defaultHost) {
        return Strings.isNullOrEmpty(cluster.getKerberosConfig().getAdminUrl()) ? defaultHost : cluster.getKerberosConfig().getAdminUrl();
    }

    public String getRealm(String gwDomain, KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getRealm()) ? gwDomain.toUpperCase() : kerberosConfig.getRealm().toUpperCase();
    }

    public String getDomains(String gwDomain) {
        return '.' + gwDomain;
    }

    public String resolveLdapUrlForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getLdapUrl()) ? null : kerberosConfig.getLdapUrl();
    }

    public String resolveContainerDnForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getContainerDn()) ? null : kerberosConfig.getContainerDn();
    }

    public String resolvePrincipalForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getPrincipal()) ? kerberosConfig.getAdmin() + PRINCIPAL
                : kerberosConfig.getPrincipal();
    }
}
