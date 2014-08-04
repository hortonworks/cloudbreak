package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.controller.InternalServerException;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service
public class CloudFormationTemplateBuilder {

    @Autowired
    private Configuration freemarkerConfiguration;

    public String build(String templatePath, int volumeCount) {
        Map<String, Object> model = new HashMap<>();
        model.put("volumeCount", volumeCount);
        try {
            return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(templatePath, "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            throw new InternalServerException("Failed to process CloudFormation freemarker template", e);
        }
    }

    @VisibleForTesting
    void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
}