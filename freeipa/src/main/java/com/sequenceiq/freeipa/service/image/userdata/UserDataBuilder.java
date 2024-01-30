package com.sequenceiq.freeipa.service.image.userdata;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
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
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.encryption.EncryptionUtil;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    private static final String SECRET_KEYS_AND_NAMES_LOCATION = "encryption/secret-keys-and-names.json";

    @Value("${cdp.apiendpoint.url:}")
    private String cdpApiEndpointUrl;

    private Map<String, String> secretKeysAndNames;

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Inject
    private EncryptionUtil encryptionUtil;

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(SECRET_KEYS_AND_NAMES_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(SECRET_KEYS_AND_NAMES_LOCATION);
                secretKeysAndNames = JsonUtil.readValue(json, new TypeReference<>() { });
                LOGGER.debug("Loaded the following secretKeysAndNames: {}.", secretKeysAndNames);
            } catch (IOException e) {
                LOGGER.warn("Secret keys and names could not be loaded!", e);
                throw e;
            }
        } else {
            LOGGER.warn("ClassPathResource for SECRET_KEYS_AND_NAMES_LOCATION does not exist, therefore secretKeysAndNames could not be loaded.");
        }
    }

    public String buildUserData(String accountId, DetailedEnvironmentResponse environment, Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters parameters, String saltBootPassword, String cbCert,
            CcmConnectivityParameters ccmConnectivityParameters, ProxyConfig proxyConfig) {
        String userData = build(accountId, environment, cloudPlatform, cbSshKeyDer, sshUser, parameters, saltBootPassword,
                cbCert, ccmConnectivityParameters, proxyConfig);
        LOGGER.debug("User data content: {}", anonymize(userData));
        return userData;
    }

    private String build(String accountId, DetailedEnvironmentResponse environment, Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters params, String saltBootPassword, String cbCert, CcmConnectivityParameters ccmConnectivityParameters, ProxyConfig proxyConfig) {
        Map<String, Object> model = new HashMap<>();
        model.put("environmentCrn", environment.getCrn());
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", true);
        model.put("signaturePublicKey", BaseEncoding.base64().encode(cbSshKeyDer));
        model.put("sshUser", sshUser);
        model.put("customUserData", userDataBuilderParams.getCustomData());
        model.put("saltBootPassword", saltBootPassword);
        model.put("cbCert", cbCert);
        model.put("cdpApiEndpointUrl", Strings.nullToEmpty(cdpApiEndpointUrl));
        extendModelWithCcmConnectivity(InstanceGroupType.GATEWAY, ccmConnectivityParameters, accountId, environment, model);
        extendModelWithProxyParams(proxyConfig, model);
        extendModelAndEncryptSecretsIfSecretEncryptionEnabled(environment, model);
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

    private void extendModelAndEncryptSecretsIfSecretEncryptionEnabled(DetailedEnvironmentResponse environment, Map<String, Object> model) {
        if (environment.isEnableSecretEncryption()) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
            model.put("secretEncryptionEnabled", Boolean.TRUE);
            switch (cloudPlatform) {
                case AWS -> model.put("secretEncryptionKeySource", environment.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn());
                default -> LOGGER.warn("Couldn't specify secret encryption key source for cloud platform {}.", cloudPlatform);
            }

            for (Entry<String, Object> entry : model.entrySet()) {
                String key = entry.getKey();
                if (secretKeysAndNames.containsKey(key)) {
                    String secretName = secretKeysAndNames.get(key);
                    String secretValue = (String) entry.getValue();
                    LOGGER.debug("Encrypting secret {}...", key);

                    if (!secretValue.isEmpty()) {
                        entry.setValue(encryptionUtil.encrypt(cloudPlatform, secretValue, environment, secretName));
                        LOGGER.debug("Succesfully encrypted secret {}.", key);
                    } else {
                        LOGGER.debug("Secret {} is an empty String, therefore skipping encryption for it.", key);
                    }
                }
            }
        }
    }

    private String build(Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("init/init.ftl", "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudConnectorException("Failed to process init script freemarker template", e);
        }
    }

    @VisibleForTesting
    void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
}
