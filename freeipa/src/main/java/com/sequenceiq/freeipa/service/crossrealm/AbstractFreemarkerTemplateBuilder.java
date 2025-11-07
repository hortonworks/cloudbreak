package com.sequenceiq.freeipa.service.crossrealm;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public abstract class AbstractFreemarkerTemplateBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreemarkerTemplateBuilder.class);

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    protected String build(String templateName, Map<String, Object> model) {
        try {
            return freeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(templateName, "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            LOGGER.error("Failed to build {} freemarker template during commands generation", templateName, e);
            throw new CloudbreakServiceException("Failed to generate commands, please contact Cloudera support!");
        }
    }
}
