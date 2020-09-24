package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Service
public class AzureStorageAccoutTemplateProviderService {

    @Value("${cb.arm.storageaccount.template.path:}")
    private String armStorageAccountTemplatePath;

    @Inject
    private Configuration freemarkerConfiguration;

    public Template getTemplate() {
        try {
            String armTemplate = freemarkerConfiguration.getTemplate(armStorageAccountTemplatePath, "UTF-8").toString();
            return new Template(armStorageAccountTemplatePath, armTemplate, freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }
}
