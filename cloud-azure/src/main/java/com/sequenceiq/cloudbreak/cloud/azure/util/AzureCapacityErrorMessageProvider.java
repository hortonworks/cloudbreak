package com.sequenceiq.cloudbreak.cloud.azure.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@Component
public class AzureCapacityErrorMessageProvider {

    private static final String INSTANCE_CAPACITY_ERROR_MESSAGE = """
            Azure was unable to allocate the machines using the selected instance type (%1$s) due to regional capacity limits.
            How to fix this: Please modify your instance type settings to select a different instance type to proceed with deployment \
            (e.g., moving from the %1$s to v5 series).
            """;

    private static final String FLEXIBLE_SERVER_CAPACITY_ERROR_MESSAGE = """
            Azure was unable to provide the requested Flexible Server type due to regional capacity limits.
            How to fix this: Please vertical scale your Flexible Server on Azure Portal: %s
            """;

    public String getInstanceCapacityErrorMessage(String instanceType) {
        return INSTANCE_CAPACITY_ERROR_MESSAGE.formatted(instanceType);
    }

    public String getFlexibleServerCapacityErrorMessage() {
        return FLEXIBLE_SERVER_CAPACITY_ERROR_MESSAGE.formatted(DocumentationLinkProvider.azureFlexibleServerVerticalScalingLink());
    }
}
