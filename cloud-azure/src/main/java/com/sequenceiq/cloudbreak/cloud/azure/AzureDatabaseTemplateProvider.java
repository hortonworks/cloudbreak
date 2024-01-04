package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Service
public class AzureDatabaseTemplateProvider {

    @Value("${cb.arm.database.template.path:}")
    private String armDatabaseTemplatePath;

    @Value("${cb.arm.flexible.database.template.path:}")
    private String armFlexibleDatabaseTemplatePath;

    @Inject
    private Configuration freemarkerConfiguration;

    public String getDBTemplateString(DatabaseStack databaseStack) {
        return getDBTemplate(databaseStack).toString();
    }

    Template getTemplate(DatabaseStack stack) {
        try {
            return new Template(chooseTemplate(stack), stack.getTemplate(), freemarkerConfiguration);
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't create template object", e);
        }
    }

    private Template getDBTemplate(DatabaseStack databaseStack) {
        try {
            return freemarkerConfiguration.getTemplate(chooseTemplate(databaseStack), "UTF-8");
        } catch (IOException e) {
            throw new CloudConnectorException("Couldn't get ARM template", e);
        }
    }

    private String chooseTemplate(DatabaseStack databaseStack) {
        return new AzureDatabaseServerView(databaseStack.getDatabaseServer()).getAzureDatabaseType().isSingleServer() ?
                armDatabaseTemplatePath : armFlexibleDatabaseTemplatePath;
    }
}
