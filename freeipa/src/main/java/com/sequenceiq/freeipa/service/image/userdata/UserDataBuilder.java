package com.sequenceiq.freeipa.service.image.userdata;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    @Value("${cdp.apiendpoint.url:}")
    private String cdpApiEndpointUrl;

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

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
