package com.sequenceiq.cloudbreak.blueprint.kerberos;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosDetailService {

    private static final String PRINCIPAL = "/admin";

    private final Gson gson = new Gson();

    @Inject
    private VaultService vaultService;

    public String resolveTypeForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        if (!Strings.isNullOrEmpty(kerberosConfig.getContainerDn()) && !Strings.isNullOrEmpty(kerberosConfig.getLdapUrl())) {
            return "active-directory";
        }
        return "mit-kdc";
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
        return Strings.isNullOrEmpty(kerberosConfig.getPrincipal()) ? vaultService.resolveSingleValue(kerberosConfig.getAdmin()) + PRINCIPAL
                : vaultService.resolveSingleValue(kerberosConfig.getPrincipal());
    }

    public boolean isAmbariManagedKerberosPackages(@Nonnull KerberosConfig kerberosConfig) throws IOException {
        if (isEmpty(kerberosConfig.getDescriptor())) {
            return true;
        }
        try {
            JsonNode node = JsonUtil.readTree(vaultService.resolveSingleValue(kerberosConfig.getDescriptor()))
                    .get("kerberos-env").get("properties").get("install_packages");
            return node.asBoolean();
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    public Map<String, Object> getKerberosEnvProperties(@Nonnull KerberosConfig kerberosConfig) {
        Map<String, Object> kerberosEnv = (Map<String, Object>) gson
                .fromJson(vaultService.resolveSingleValue(kerberosConfig.getDescriptor()), Map.class).get("kerberos-env");
        return (Map<String, Object>) kerberosEnv.get("properties");
    }
}
