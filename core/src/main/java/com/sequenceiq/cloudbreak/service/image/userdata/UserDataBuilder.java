package com.sequenceiq.cloudbreak.service.image.userdata;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

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

    public Map<InstanceGroupType, String> buildUserData(Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters parameters, String saltBootPassword, String cbCert, CcmParameters ccmParameters, ProxyConfig proxyConfig) {
        Map<InstanceGroupType, String> result = new EnumMap<>(InstanceGroupType.class);
        for (InstanceGroupType type : InstanceGroupType.values()) {
            String userData = build(type, cloudPlatform, cbSshKeyDer, sshUser, parameters, saltBootPassword, cbCert, ccmParameters, proxyConfig);
            result.put(type, userData);
            LOGGER.debug("User data for {}, content; {}", type, userData);
        }
        return result;
    }

    private String build(InstanceGroupType type, Platform cloudPlatform, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters params, String saltBootPassword, String cbCert, CcmParameters ccmParameters, ProxyConfig proxyConfig) {
        Map<String, Object> model = new HashMap<>();
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", type == InstanceGroupType.GATEWAY);
        model.put("tmpSshKey", "#NOT_USER_ANYMORE_BUT_KEEP_FOR_BACKWARD_COMPATIBILITY");
        model.put("signaturePublicKey", BaseEncoding.base64().encode(cbSshKeyDer));
        model.put("sshUser", sshUser);
        model.put("customUserData", userDataBuilderParams.getCustomData());
        model.put("saltBootPassword", saltBootPassword);
        model.put("cbCert", cbCert);
        CcmParameters.addToTemplateModel(ccmParameters, model);
        extendModelWithProxyParams(type, proxyConfig, model);
        return build(model);
    }

    private void extendModelWithProxyParams(InstanceGroupType type, ProxyConfig proxyConfig, Map<String, Object> model) {
        if (type == InstanceGroupType.GATEWAY && proxyConfig != null) {
            model.put("proxyEnabled", Boolean.TRUE);
            model.put("proxyHost", proxyConfig.getServerHost());
            model.put("proxyPort", proxyConfig.getServerPort().toString());
            proxyConfig.getProxyAuthentication().ifPresent(auth -> {
                model.put("proxyUser", auth.getUserName());
                model.put("proxyPassword", auth.getPassword());
            });
            LOGGER.info("Proxy config set up for gateway instances' userdata script: {}", proxyConfig);
        } else {
            model.put("proxyEnabled", Boolean.FALSE);
            LOGGER.info("No proxy config set up for {} instances' userdata script", type);
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
