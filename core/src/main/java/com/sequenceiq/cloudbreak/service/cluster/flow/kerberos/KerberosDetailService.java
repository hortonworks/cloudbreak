package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    public String resolveTypeForKerberos(KerberosConfig kerberosConfig) {
        if (!Strings.isNullOrEmpty(kerberosConfig.getKerberosContainerDn()) && !Strings.isNullOrEmpty(kerberosConfig.getKerberosLdapUrl())) {
            return "active-directory";
        } else if (!Strings.isNullOrEmpty(kerberosConfig.getKerberosUrl())) {
            return "mit-kdc";
        }
        return "mit-kdc";
    }

    public String resolveHostForKerberos(Cluster cluster, String defaultHost) {
        return Strings.isNullOrEmpty(cluster.getKerberosConfig().getKerberosUrl()) ? defaultHost : cluster.getKerberosConfig().getKerberosUrl();
    }

    public String resolveHostForKdcAdmin(Cluster cluster, String defaultHost) {
        return Strings.isNullOrEmpty(cluster.getKerberosConfig().getKdcAdminUrl()) ? defaultHost : cluster.getKerberosConfig().getKdcAdminUrl();
    }

    public String getRealm(String gwDomain, KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosRealm()) ? gwDomain.toUpperCase() : kerberosConfig.getKerberosRealm().toUpperCase();
    }

    public String getDomains(String gwDomain) {
        return '.' + gwDomain;
    }

    public String resolveLdapUrlForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosLdapUrl()) ? null : kerberosConfig.getKerberosLdapUrl();
    }

    public String resolveContainerDnForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosContainerDn()) ? null : kerberosConfig.getKerberosContainerDn();
    }

    public String resolvePrincipalForKerberos(KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getKerberosPrincipal()) ? kerberosConfig.getKerberosAdmin() + PRINCIPAL
                : kerberosConfig.getKerberosPrincipal();
    }

    public boolean isAmbariManagedKrb5Conf(KerberosConfig kerberosConfig) throws IOException {
        if (!StringUtils.hasLength(kerberosConfig.getKrb5Conf())) {
            return true;
        }
        try {
            JsonNode node = JsonUtil.readTree(kerberosConfig.getKrb5Conf()).get("krb5-conf").get("properties").get("manage_krb5_conf");
            return node.asBoolean();
        } catch (NullPointerException ignored) {
            return true;
        }
    }
}
