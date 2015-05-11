package com.sequenceiq.cloudbreak.service.stack.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Component
public class UserDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataBuilder.class);

    @Autowired
    private Configuration freemarkerConfiguration;

    public Map<InstanceGroupType, String> buildUserData(CloudPlatform cloudPlatform) {
        Map<InstanceGroupType, String> result = new HashMap<>();
        result.put(InstanceGroupType.GATEWAY, build(cloudPlatform, true));
        result.put(InstanceGroupType.CORE, build(cloudPlatform, false));
        return result;
    }

    private String build(CloudPlatform cloudPlatform, boolean gateway) {
        Map<String, Object> model = new HashMap<>();
        model.put("platformDiskPrefix", cloudPlatform.getDiskPrefix());
        model.put("platformDiskStartLabel", cloudPlatform.startLabel());
        model.put("gateway", gateway);
        try {
            return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("init/init.ftl", "UTF-8"), model);
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
