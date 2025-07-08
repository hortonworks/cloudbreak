package com.sequenceiq.cloudbreak.service.image.userdata;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private StackEncryptionService stackEncryptionService;

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

    private String build(Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("init/init.ftl", "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudConnectorException("Failed to process init script freemarker template", e);
        }
    }
}
