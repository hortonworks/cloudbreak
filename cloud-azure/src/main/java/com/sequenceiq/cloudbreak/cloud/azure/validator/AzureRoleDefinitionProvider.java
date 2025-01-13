package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureRoleDefinitionProvider {

    private static final String AZURE_FLEXIBLE_MINIMAL_ROLE_DEF_JSON_LOCATION = "definitions/azure-flexible-minimal-role-def.json";

    private static final String AZURE_CMK_MINIMAL_ROLE_DEF_JSON_LOCATION = "definitions/azure-cmk-minimal-role-def.json";

    AzureRoleDefinitionProperties loadAzureFlexibleMinimalRoleDefinition() {
        return loadAzureRoleDefinition(AZURE_FLEXIBLE_MINIMAL_ROLE_DEF_JSON_LOCATION);
    }

    AzureRoleDefinitionProperties loadAzureCMKMinimalRoleDefinition() {
        return loadAzureRoleDefinition(AZURE_CMK_MINIMAL_ROLE_DEF_JSON_LOCATION);
    }

    AzureRoleDefinitionProperties loadAzureRoleDefinition(String resourcePath) {
        ClassPathResource classPathResource = new ClassPathResource(resourcePath);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(resourcePath);
                return JsonUtil.readValue(json, AzureRoleDefinitionProperties.class);
            } catch (IOException e) {
                throw new CloudbreakServiceException("Failed to load Azure Flexible server minimal role definition json!", e);
            }
        }
        throw new CloudbreakServiceException("Azure Flexible server minimal role definition json file could not found!");
    }
}
