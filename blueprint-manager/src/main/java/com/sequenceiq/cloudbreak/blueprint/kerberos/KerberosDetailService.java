package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    private final Gson gson = new Gson();

    public String resolveTypeForKerberos(KerberosConfig kerberosConfig) {
        if (!Strings.isNullOrEmpty(kerberosConfig.getContainerDn()) && !Strings.isNullOrEmpty(kerberosConfig.getLdapUrl())) {
            return "active-directory";
        }
        return "mit-kdc";
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

    public boolean isAmbariManagedKerberosPackages(KerberosConfig kerberosConfig) throws IOException {
        if (!StringUtils.hasLength(kerberosConfig.getDescriptor())) {
            return true;
        }
        try {
            JsonNode node = JsonUtil.readTree(kerberosConfig.getDescriptor()).get("kerberos-env").get("properties").get("install_packages");
            return node.asBoolean();
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    public Map<String, Object> getKerberosEnvProperties(KerberosConfig kerberosConfig) {
        Map<String, Object> kerberosEnv = (Map<String, Object>) gson.fromJson(kerberosConfig.getDescriptor(), Map.class).get("kerberos-env");
        return (Map<String, Object>) kerberosEnv.get("properties");
    }
}
