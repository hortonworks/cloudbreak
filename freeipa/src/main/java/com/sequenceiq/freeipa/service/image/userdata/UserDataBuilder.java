package com.sequenceiq.freeipa.service.image.userdata;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_2;
import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;
import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
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

import com.google.common.base.Strings;
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
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileConverter;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.client.CachedEncryptionProfileClientService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    private static final String SALTBOOT_TLS_VERSION_1_2 = "1.2";

    private static final String SALTBOOT_TLS_VERSION_1_3 = "1.3";

    @Value("${cdp.apiendpoint.url:}")
    private String cdpApiEndpointUrl;

    @Value("${freeipa.saltboot.httpsOnly:true}")
    private boolean saltbootHttpsOnly;

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private CachedEncryptionProfileClientService encryptionProfileClientService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CipherSuiteProvider cipherSuiteProvider;

    public String buildUserData(Stack stack, DetailedEnvironmentResponse environment, Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters parameters, String saltBootPassword, String cbCert,
            CcmConnectivityParameters ccmConnectivityParameters, ProxyConfig proxyConfig) {
        String userData = build(stack, environment, cloudPlatform, cbSshKeyDer, sshUser, parameters, saltBootPassword,
                cbCert, ccmConnectivityParameters, proxyConfig);
        LOGGER.debug("User data content: {}", userData);
        return userData;
    }

    private String build(Stack stack, DetailedEnvironmentResponse environment, Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters params, String saltBootPassword, String cbCert, CcmConnectivityParameters ccmConnectivityParameters,
            ProxyConfig proxyConfig) {
        Map<String, Object> model = new HashMap<>();
        model.put("environmentCrn", environment.getCrn());
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", true);
        model.put("signaturePublicKey", BaseEncoding.base64().encode(cbSshKeyDer));
        model.put("sshUser", sshUser);
        model.put("customUserData", userDataBuilderParams.getCustomUserData());
        model.put("saltBootPassword", saltBootPassword);
        model.put("cbCert", cbCert);
        model.put("cdpApiEndpointUrl", Strings.nullToEmpty(cdpApiEndpointUrl));
        extendModelWithCcmConnectivity(InstanceGroupType.GATEWAY, ccmConnectivityParameters, stack.getAccountId(), environment, model);
        extendModelWithProxyParams(proxyConfig, model);
        extendModelWithSecretEncryptionParams(environment, stack.getId(), model);
        extendModelWithSaltbootTlsVersion(environment, model);
        if (saltbootHttpsOnly) {
            model.put("saltbootHttpsOnly", Boolean.TRUE);
        }
        return build(model);
    }

    private void extendModelWithCcmConnectivity(InstanceGroupType type, CcmConnectivityParameters ccmConnectivityParameters,
            String accountId, DetailedEnvironmentResponse environment, Map<String, Object> model) {
        if (CcmConnectivityMode.CCMV1.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmParameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmParameters(), model);
        } else if (CcmConnectivityMode.CCMV2.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmV2Parameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmV2Parameters(), model);
        } else if (CcmConnectivityMode.CCMV2_JUMPGATE.equals(ccmConnectivityParameters.getConnectivityMode())) {
            CcmV2JumpgateParameters.addToTemplateModel(type, ccmConnectivityParameters.getCcmV2JumpgateParameters(), model);
            removeIfNotEntitledOrForced(type, environment, model);
        } else {
            model.put(CcmParameterConstants.CCM_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2JumpgateParameterConstants.CCMV2_JUMPGATE_ENABLED_KEY, Boolean.FALSE);
        }
    }

    private void removeIfNotEntitledOrForced(InstanceGroupType type, DetailedEnvironmentResponse environment, Map<String, Object> model) {
        if (isGateway(type)) {
            if (CcmV2TlsType.TWO_WAY_TLS == ccmV2TlsTypeDecider.decide(environment)) {
                model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID, EMPTY);
                model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY, EMPTY);
                model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_KEY, EMPTY);
                model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_IV, EMPTY);
                model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_FOR_PRIVATE_KEY, EMPTY);
            }
        }
    }

    private void extendModelWithProxyParams(ProxyConfig proxyConfig, Map<String, Object> model) {
        if (proxyConfig != null) {
            model.put("proxyEnabled", Boolean.TRUE);
            model.put("proxyHost", proxyConfig.getServerHost());
            model.put("proxyPort", proxyConfig.getServerPort().toString());
            model.put("proxyProtocol", proxyConfig.getProtocol());
            proxyConfig.getProxyAuthentication().ifPresent(auth -> {
                model.put("proxyUser", auth.getUserName());
                model.put("proxyPassword", auth.getPassword());
            });
            model.put("proxyNoProxyHosts", proxyConfig.getNoProxyHosts());
            LOGGER.info("Proxy config set up for freeipa instances' userdata script: {}", anonymize(proxyConfig.toString()));
        } else {
            model.put("proxyEnabled", Boolean.FALSE);
            LOGGER.info("No proxy config set up for freeipa instances' userdata script");
        }
    }

    private void extendModelWithSecretEncryptionParams(DetailedEnvironmentResponse environment, Long stackId, Map<String, Object> model) {
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
            EncryptionProfileResponse profile = encryptionProfileClientService.getByCrnOrDefaultIfEmpty(encryptionProfileCrn);
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
