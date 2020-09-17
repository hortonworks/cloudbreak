package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Service
public class AzureDatabaseTemplateProvider {

    @Value("${cb.arm.database.template.path:}")
    private String armDatabaseTemplatePath;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String getDBTemplateString() {
        return getDBTemplate().toString();
    }

    Template getTemplate(DatabaseStack stack) {
        try {
            return new Template(chooseTemplate(), stack.getTemplate(), freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }

    private Template getDBTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(chooseTemplate(), "UTF-8");
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't get ARM template", e);
        }
    }

    private String chooseTemplate() {
        return armDatabaseTemplatePath;
    }
}
