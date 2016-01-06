package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    @Value("${cb.docker.relocate:}")
    private Boolean relocateDocker;

    @Inject
    private Configuration freemarkerConfiguration;

    Map<InstanceGroupType, String> buildUserData(Platform cloudPlatform, String pubKey, String tmpSshKey, String sshUser, PlatformParameters parameters) {
        Map<InstanceGroupType, String> result = new HashMap<>();
        result.put(InstanceGroupType.GATEWAY, build(InstanceGroupType.GATEWAY, cloudPlatform, pubKey, tmpSshKey, sshUser, parameters));
        result.put(InstanceGroupType.CORE, build(InstanceGroupType.CORE, cloudPlatform, pubKey, tmpSshKey, sshUser, parameters));
        return result;
    }

    private String build(InstanceGroupType type, Platform cloudPlatform, String publicSssKey, String tmpSshKey, String sshUser, PlatformParameters params) {
        Map<String, Object> model = new HashMap<>();
        model.put("cloudPlatform", cloudPlatform.value());
        model.put("platformDiskPrefix", params.scriptParams().getDiskPrefix());
        model.put("platformDiskStartLabel", params.scriptParams().getStartLabel());
        model.put("gateway", type == InstanceGroupType.GATEWAY);
        model.put("tmpSshKey", tmpSshKey);
        model.put("sshUser", sshUser);
        model.put("publicSshKey", publicSssKey);
        model.put("relocateDocker", relocateDocker);
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
    void setRelocateDocker(Boolean relocateDocker) {
        this.relocateDocker = relocateDocker;
    }

    @VisibleForTesting
    void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
}
