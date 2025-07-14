package com.sequenceiq.cloudbreak.kerberos;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@Component
public class KerberosPillarConfigGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosPillarConfigGenerator.class);

    @Value("${cb.kerberos.secret.cCache.location}")
    private String defaultKerberosCcacheSecretLocation;

    @Value("${cb.cm.kerberos.encryption.type}")
    private String defaultKerberosEncryptionType;

    @Value("${cb.kerberos.secret.location}")
    private String kerberosSecretLocation;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private FreeipaClientService freeipaClient;

    public Map<String, SaltPillarProperties> createKerberosPillar(KerberosConfig kerberosConfig, DetailedEnvironmentResponse detailedEnvironmentResponse)
            throws IOException {
        if (isKerberosNeeded(kerberosConfig)) {
            Map<String, String> kerberosPillarConf = new HashMap<>();
            if (isEmpty(kerberosConfig.getDescriptor())) {
                putIfNotNull(kerberosPillarConf, kerberosConfig.getUrl(), "url");
                putIfNotNull(kerberosPillarConf, kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, kerberosConfig.getUrl()), "adminUrl");
                putIfNotNull(kerberosPillarConf, kerberosConfig.getRealm(), "realm");
            } else {
                Map<String, Object> properties = kerberosDetailService.getKerberosEnvProperties(kerberosConfig);
                putIfNotNull(kerberosPillarConf, properties.get("kdc_hosts"), "url");
                putIfNotNull(kerberosPillarConf, properties.get("admin_server_host"), "adminUrl");
                putIfNotNull(kerberosPillarConf, properties.get("realm"), "realm");
            }
            putIfNotNull(kerberosPillarConf, defaultKerberosEncryptionType, "encryptionType");
            putIfNotNull(kerberosPillarConf, defaultKerberosCcacheSecretLocation, "cCacheSecretLocation");
            putIfNotNull(kerberosPillarConf, kerberosSecretLocation, "kerberosSecretLocation");
            putIfNotNull(kerberosPillarConf, kerberosConfig.getVerifyKdcTrust().toString(), "verifyKdcTrust");
            putIfNotNull(kerberosPillarConf, kerberosConfig.getContainerDn(), "container-dn");
            Map<String, Object> trustPillars = createTrustPillars(detailedEnvironmentResponse);
            return Map.of("kerberos", new SaltPillarProperties("/kerberos/init.sls",
                    Map.of("kerberos", kerberosPillarConf,
                            "trust", trustPillars))
            );
        } else {
            return Map.of();
        }
    }

    private Map<String, Object> createTrustPillars(DetailedEnvironmentResponse detailedEnvironmentResponse) {
        if (EnvironmentType.isHybridFromEnvironmentTypeString(detailedEnvironmentResponse.getEnvironmentType())) {
            TrustResponse trustResponse = freeipaClient.findByEnvironmentCrn(detailedEnvironmentResponse.getCrn())
                    .map(DescribeFreeIpaResponse::getTrust)
                    .orElse(null);
            if (trustResponse != null && StringUtils.isNotBlank(trustResponse.getRealm())) {
                LOGGER.debug("Creating trust kerberos pillar configuration for realm: {}", trustResponse.getRealm());
                return Map.of("realm", trustResponse.getRealm().toUpperCase(Locale.ROOT),
                                "domain", trustResponse.getRealm().toLowerCase(Locale.ROOT));
            } else {
                LOGGER.warn("Could not find trust realm for crn: {}", detailedEnvironmentResponse.getCrn());
                return Map.of();
            }
        } else {
            return Map.of();
        }
    }

    private boolean isKerberosNeeded(KerberosConfig kerberosConfig) throws IOException {
        return kerberosConfig != null
                && kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)
                && !kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig);
    }

    private void putIfNotNull(Map<String, String> context, Object variable, String key) {
        if (variable != null) {
            context.put(key, variable.toString());
        }
    }
}
