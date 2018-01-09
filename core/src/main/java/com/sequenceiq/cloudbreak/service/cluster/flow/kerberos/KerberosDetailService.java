package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    private final Gson gson = new Gson();

    public String resolveTypeForKerberos(KerberosConfig kerberosConfig) {
        if (!Strings.isNullOrEmpty(kerberosConfig.getKerberosContainerDn()) && !Strings.isNullOrEmpty(kerberosConfig.getKerberosLdapUrl())) {
            return "active-directory";
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

    public boolean isAmbariManagedKerberosPackages(KerberosConfig kerberosConfig) throws IOException {
        if (!StringUtils.hasLength(kerberosConfig.getKerberosDescriptor())) {
            return true;
        }
        try {
            JsonNode node = JsonUtil.readTree(kerberosConfig.getKerberosDescriptor()).get("kerberos-env").get("properties").get("install_packages");
            return node.asBoolean();
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    public Map<String, Object> getKerberosEnvProperties(KerberosConfig kerberosConfig) {
        Map<String, Object> kerberosEnv = (Map<String, Object>) gson.fromJson(kerberosConfig.getKerberosDescriptor(), Map.class).get("kerberos-env");
        return (Map<String, Object>) kerberosEnv.get("properties");
    }
}
