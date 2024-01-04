package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.model.AzureDatabaseType;

import freemarker.template.TemplateException;

@Service
public class AzureDatabaseTemplateBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseTemplateBuilder.class);

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Inject
    private Map<AzureDatabaseType, AzureDatabaseTemplateModelBuilder> azureDatabaseTemplateModelBuilderMap;

    public String build(CloudContext cloudContext, DatabaseStack databaseStack) {
        try {
            AzureNetworkView azureNetworkView = new AzureNetworkView(databaseStack.getNetwork());
            AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(databaseStack.getDatabaseServer());
            Map<String, Object>  model = azureDatabaseTemplateModelBuilderMap.get(azureDatabaseServerView.getAzureDatabaseType())
                    .buildModel(azureDatabaseServerView, azureNetworkView, databaseStack);
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(azureDatabaseTemplateProvider.getTemplate(databaseStack), model);
            LOGGER.debug("Generated ARM database template: {}", AnonymizerUtil.anonymize(generatedTemplate));
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the ARM TemplateBuilder", e);
        }
    }
}
