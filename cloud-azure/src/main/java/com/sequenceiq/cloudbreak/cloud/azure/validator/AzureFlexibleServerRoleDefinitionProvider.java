package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureFlexibleServerRoleDefinitionProvider {

    private static final String AZURE_FLEXIBLE_MINIMAL_ROLE_DEF_JSON_LOCATION = "definitions/azure-flexible-minimal-role-def.json";

    AzureRoleDefinitionProperties loadAzureFlexibleMinimalRoleDefinition() {
        ClassPathResource classPathResource = new ClassPathResource(AZURE_FLEXIBLE_MINIMAL_ROLE_DEF_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AZURE_FLEXIBLE_MINIMAL_ROLE_DEF_JSON_LOCATION);
                return JsonUtil.readValue(json, AzureRoleDefinitionProperties.class);
            } catch (IOException e) {
                throw new CloudbreakServiceException("Failed to load Azure Flexible server minimal role definition json!", e);
            }
        }
        throw new CloudbreakServiceException("Azure Flexible server minimal role definition json file could not found!");
    }
}
