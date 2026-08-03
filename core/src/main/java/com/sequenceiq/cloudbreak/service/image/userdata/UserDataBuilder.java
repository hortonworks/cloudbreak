package com.sequenceiq.cloudbreak.service.image.userdata;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityMode;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterConstants;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterConstants;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileConverter;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    private static final String SALTBOOT_TLS_VERSION_1_2 = "1.2";

    private static final String SALTBOOT_TLS_VERSION_1_3 = "1.3";

    @Value("${cb.saltboot.httpsOnly:true}")
    private boolean saltbootHttpsOnly;

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private EncryptionProfileService encryptionProfileService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CipherSuiteProvider cipherSuiteProvider;

    public Map<InstanceGroupType, String> buildUserData(Platform cloudPlatform, Variant variant, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters parameters, String saltBootPassword, String cbCert, CcmConnectivityParameters ccmParameters, ProxyConfig proxyConfig,
            DetailedEnvironmentResponse environment, Long stackId) {
        Map<InstanceGroupType, String> result = new EnumMap<>(InstanceGroupType.class);
        for (InstanceGroupType type : InstanceGroupType.values()) {
            String userData =
                    build(type, cloudPlatform, variant, cbSshKeyDer, sshUser, parameters, saltBootPassword, cbCert, ccmParameters, proxyConfig, environment,
                            stackId);
            result.put(type, userData);
            LOGGER.debug("User data for {}, content; {}", type, anonymize(userData));
        }
        return result;
    }

    private String build(InstanceGroupType type, Platform cloudPlatform, Variant variant, byte[] cbSshKeyDer, String sshUser, PlatformParameters params,
            String saltBootPassword, String cbCert, CcmConnectivityParameters ccmConnectivityParameters, ProxyConfig proxyConfig,
            DetailedEnvironmentResponse environment, Long stackId) {
        Map<String, Object> model = new HashMap<>();
        model.put("environmentCrn", environment.getCrn());
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", type == InstanceGroupType.GATEWAY);
        model.put("tmpSshKey", "#NOT_USER_ANYMORE_BUT_KEEP_FOR_BACKWARD_COMPATIBILITY");
        model.put("signaturePublicKey", BaseEncoding.base64().encode(cbSshKeyDer));
        model.put("sshUser", sshUser);
        model.put("customUserData", userDataBuilderParams.getCustomUserData());
        model.put("saltBootPassword", saltBootPassword);
        model.put("cbCert", cbCert);
        extendModelWithCcmConnectivity(type, ccmConnectivityParameters, model);
        extendModelWithProxyParams(type, proxyConfig, model);
        extendModelAndEncryptSecretsIfSecretEncryptionEnabled(environment, stackId, model);
        extendModelWithSaltbootTlsVersion(environment, model);
        if (saltbootHttpsOnly) {
            model.put("saltbootHttpsOnly", Boolean.TRUE);
        }
        return build(model);
    }

    private void extendModelWithCcmConnectivity(InstanceGroupType type, CcmConnectivityParameters ccmConnectivityParameters, Map<String, Object> model) {
        if (CcmConnectivityMode.CCMV1.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmParameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmParameters(), model);
        } else if (CcmConnectivityMode.CCMV2.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmV2Parameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmV2Parameters(), model);
        } else if (CcmConnectivityMode.CCMV2_JUMPGATE.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmV2JumpgateParameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmV2JumpgateParameters(), model);
        } else {
            model.put(CcmParameterConstants.CCM_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2JumpgateParameterConstants.CCMV2_JUMPGATE_ENABLED_KEY, Boolean.FALSE);
        }
    }

    private void extendModelWithProxyParams(InstanceGroupType type, ProxyConfig proxyConfig, Map<String, Object> model) {
        if (type == InstanceGroupType.GATEWAY && proxyConfig != null) {
            model.put("proxyEnabled", Boolean.TRUE);
            model.put("proxyHost", proxyConfig.getServerHost());
            model.put("proxyPort", proxyConfig.getServerPort().toString());
            model.put("proxyProtocol", proxyConfig.getProtocol());
            proxyConfig.getProxyAuthentication().ifPresent(auth -> {
                model.put("proxyUser", auth.getUserName());
                model.put("proxyPassword", auth.getPassword());
            });
            model.put("proxyNoProxyHosts", proxyConfig.getNoProxyHosts());
            LOGGER.info("Proxy config set up for gateway instances' userdata script: {}", anonymize(proxyConfig.toString()));
        } else {
            model.put("proxyEnabled", Boolean.FALSE);
            LOGGER.info("No proxy config set up for {} instances' userdata script", type);
        }
    }

    private void extendModelAndEncryptSecretsIfSecretEncryptionEnabled(DetailedEnvironmentResponse environment, Long stackId, Map<String, Object> model) {
        if (environment.isEnableSecretEncryption()) {
            StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stackId);
            model.put("secretEncryptionEnabled", Boolean.TRUE);
            model.put("secretEncryptionKeySource", stackEncryption.getEncryptionKeyLuks());
        }
    }

    private void extendModelWithSaltbootTlsVersion(DetailedEnvironmentResponse environment, Map<String, Object> model) {
        String encryptionProfileCrn = environment.getEncryptionProfileCrn();
        if (StringUtils.isNotBlank(encryptionProfileCrn)
                && entitlementService.isConfigureEncryptionProfileEnabled(environment.getAccountId())) {
            EncryptionProfileResponse profile = encryptionProfileService.getEncryptionProfileByCrnOrDefault(encryptionProfileCrn);
            determineTlsVersionBound(profile, TLS_1_2, SALTBOOT_TLS_VERSION_1_3).ifPresent(v -> model.put("saltbootMinTlsVersion", v));
            determineTlsVersionBound(profile, TLS_1_3, SALTBOOT_TLS_VERSION_1_2).ifPresent(v -> model.put("saltbootMaxTlsVersion", v));
            determineSaltbootCipherSuites(profile).ifPresent(ciphers -> model.put("saltbootCipherSuites", ciphers));
            if (shouldEnableSaltbootFipsMode(profile)) {
                model.put("saltbootFipsOnly", Boolean.TRUE);
            }
        }
    }

    private Optional<String> determineTlsVersionBound(EncryptionProfileResponse profile, TlsVersion excluded, String boundValue) {
        Set<String> tlsVersions = profile.getTlsVersions();
        if (CollectionUtils.isEmpty(tlsVersions) || tlsVersions.contains(excluded.getVersion())) {
            return Optional.empty();
        }
        TlsVersion other = excluded == TLS_1_2 ? TLS_1_3 : TLS_1_2;
        if (!tlsVersions.contains(other.getVersion())) {
            LOGGER.warn("Encryption profile '{}' contains no recognized TLS versions: {}", profile.getName(), tlsVersions);
            return Optional.empty();
        }
        LOGGER.info("Encryption profile '{}' does not allow {}, setting saltboot TLS bound to {}", profile.getName(), excluded.getVersion(), boundValue);
        return Optional.of(boundValue);
    }

    private Optional<String> determineSaltbootCipherSuites(EncryptionProfileResponse profile) {
        Map<String, List<String>> cipherSuites = profile.getCipherSuites();
        if (MapUtils.isEmpty(cipherSuites)) {
            return Optional.empty();
        }
        List<String> tls12Ciphers = cipherSuites.get(TLS_1_2.getVersion());
        if (CollectionUtils.isEmpty(tls12Ciphers)) {
            return Optional.empty();
        }
        String joined = String.join(",", tls12Ciphers);
        LOGGER.info("Encryption profile '{}' specifies TLS 1.2 cipher suites for saltboot: {}", profile.getName(), joined);
        return Optional.of(joined);
    }

    // Go's crypto/tls does not allow restricting TLS 1.3 cipher suites via tls.Config.CipherSuites.
    // GODEBUG=fips140=only is the only runtime mechanism to block TLS_CHACHA20_POLY1305_SHA256.
    private boolean shouldEnableSaltbootFipsMode(EncryptionProfileResponse profile) {
        Map<String, List<String>> cipherSuites = profile.getCipherSuites();
        if (MapUtils.isEmpty(cipherSuites)) {
            return false;
        }
        List<String> tls13Ciphers = cipherSuites.get(TLS_1_3.getVersion());
        if (CollectionUtils.isEmpty(tls13Ciphers)) {
            return false;
        }
        boolean fipsOnly = EncryptionProfileConverter.toListString(cipherSuiteProvider.getFips1403ApprovedTls13CipherSuites()).containsAll(tls13Ciphers);
        if (fipsOnly) {
            LOGGER.info("Encryption profile '{}' specifies only FIPS-approved TLS 1.3 ciphers, enabling FIPS mode for saltboot", profile.getName());
        }
        return fipsOnly;
    }

    private String build(Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("init/init.ftl", "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudConnectorException("Failed to process init script freemarker template", e);
        }
    }
}
