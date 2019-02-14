package com.sequenceiq.cloudbreak.clusterdefinition.kerberos;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    private final Gson gson = new Gson();

    public String resolveTypeForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        switch (kerberosConfig.getType()) {
            case ACTIVE_DIRECTORY:
                return "active-directory";
            case FREEIPA:
                return "ipa";
            default:
                return "mit-kdc";
        }
    }

    public String resolveHostForKerberos(@Nonnull KerberosConfig kerberosConfig, String defaultHost) {
        String host = Optional.ofNullable(kerberosConfig.getUrl()).orElse("").trim();
        return host.isEmpty() ? defaultHost : host;
    }

    public String resolveHostForKdcAdmin(@Nonnull KerberosConfig kerberosConfig, String defaultHost) {
        String adminHost = Optional.ofNullable(kerberosConfig.getAdminUrl()).orElse("").trim();
        return adminHost.isEmpty() ? defaultHost : adminHost;
    }

    public String getRealm(@Nonnull String gwDomain, @Nonnull KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getRealm()) ? gwDomain.toUpperCase() : kerberosConfig.getRealm().toUpperCase();
    }

    public String getDomains(String gwDomain) {
        return '.' + gwDomain;
    }

    public String resolveLdapUrlForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getLdapUrl()) ? null : kerberosConfig.getLdapUrl();
    }

    public String resolveContainerDnForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getContainerDn()) ? null : kerberosConfig.getContainerDn();
    }

    public String resolvePrincipalForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getPrincipal()) ? kerberosConfig.getAdmin() + PRINCIPAL
                : kerberosConfig.getPrincipal();
    }

    public boolean isAmbariManagedKerberosPackages(@Nonnull KerberosConfig kerberosConfig) throws IOException {
        return getBooleanConfigValue(kerberosConfig.getDescriptor(), new String[] { "kerberos-env", "properties", "install_packages" }, true);
    }

    public boolean isAmbariManagedKrb5Conf(@Nonnull KerberosConfig kerberosConfig) throws IOException {
        return getBooleanConfigValue(kerberosConfig.getKrb5Conf(), new String[] { "krb5-conf", "properties", "manage_krb5_conf" }, false);
    }

    private boolean getBooleanConfigValue(String configJson, String[] path, boolean defaultValue) throws IOException {
        if (isEmpty(configJson)) {
            return defaultValue;
        }
        JsonNode node = JsonUtil.readTree(configJson);
        for (String p : path) {
            node = node.get(p);
            if (node == null) {
                return defaultValue;
            }
        }
        return node.asBoolean();
    }

    public Map<String, Object> getKerberosEnvProperties(@Nonnull KerberosConfig kerberosConfig) {
        Map<String, Object> kerberosEnv = (Map<String, Object>) gson
                .fromJson(kerberosConfig.getDescriptor(), Map.class).get("kerberos-env");
        return (Map<String, Object>) kerberosEnv.get("properties");
    }

    public Map<String, Object> getKrb5ConfProperties(@Nonnull KerberosConfig kerberosConfig) {
        Map<String, Object> krb5Conf = (Map<String, Object>) gson.fromJson(kerberosConfig.getKrb5Conf(), Map.class).get("krb5-conf");
        return (Map<String, Object>) krb5Conf.get("properties");
    }
}
