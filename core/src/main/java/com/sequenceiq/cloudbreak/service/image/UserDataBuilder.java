package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    @Inject
    private UserDataBuilderParams userDataBuilderParams;

    @Inject
    private Configuration freemarkerConfiguration;

    Map<InstanceGroupType, String> buildUserData(Platform cloudPlatform, String pubKey, byte[] cbSshKeyDer, String cbSshKey, String sshUser,
            PlatformParameters parameters, Boolean relocate, String saltBootPassword) {
        Map<InstanceGroupType, String> result = new HashMap<>();
        for (InstanceGroupType type : InstanceGroupType.values()) {
            String userData = build(type, cloudPlatform, pubKey, cbSshKey, cbSshKeyDer, sshUser, parameters, relocate, saltBootPassword);
            result.put(type, userData);
            LOGGER.debug("User data for {}, content; {}", type, userData);
        }

        return result;
    }

    private String build(InstanceGroupType type, Platform cloudPlatform, String publicSssKey, String cbSshKey, byte[] cbSshKeyDer, String sshUser,
            PlatformParameters params, Boolean relocate, String saltBootPassword) {
        Map<String, Object> model = new HashMap<>();
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", type == InstanceGroupType.GATEWAY);
        model.put("tmpSshKey", cbSshKey);
        model.put("signaturePublicKey", BaseEncoding.base64().encode(cbSshKeyDer));
        model.put("sshUser", sshUser);
        model.put("publicSshKey", publicSssKey);
        model.put("customUserData", userDataBuilderParams.getCustomData());
        model.put("relocateDocker", relocate);
        model.put("saltBootPassword", saltBootPassword);
        return build(model);
    }

    private String build(Map<String, Object> model) {
        try {
            return processTemplateIntoString(freemarkerConfiguration.getTemplate("init/init.ftl", "UTF-8"), model);
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
