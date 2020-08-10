package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.StorageAccountParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.TemplateException;

@Component
public class AzureStorageAccountTemplateBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageAccountTemplateBuilder.class);

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private AzureStorageAccoutTemplateProviderService azureStorageAccoutTemplateProviderService;

    public String build(StorageAccountParameters storageAccountParameters) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("storageAccountName", storageAccountParameters.getStorageAccountName());
            model.put("location", storageAccountParameters.getStorageLocation());
            model.put("skuName", storageAccountParameters.getStorageAccountSkuType().name().toString());
            model.put("encrypted", storageAccountParameters.getEncrypted());
            model.put("userDefinedTags", storageAccountParameters.getTags());
            String generatedTemplate = freeMarkerTemplateUtils.processTemplateIntoString(azureStorageAccoutTemplateProviderService.getTemplate(), model);
            LOGGER.info("Generated storage account Arm template: {}", generatedTemplate);
            return generatedTemplate;
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process the storage account ARM Template", e);
        }
    }
}
