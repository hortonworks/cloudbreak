package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureVirtualMachineTypeProvider;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureAcceleratedNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAcceleratedNetworkValidator.class);

    private static final String AZURE_ACCELERATED_NETWORK_SUPPORT_JSON = "definitions/azure-accelerated-network-support.json";

    @Inject
    private AzureVirtualMachineTypeProvider azureVirtualMachineTypeProvider;

    public Map<String, Boolean> validate(AzureStackView azureStackView) {
        return getVmTypes(azureStackView).stream().collect(Collectors.toMap(vm -> vm, this::isSupportedForVm));
    }

    private Set<String> getVmTypes(AzureStackView azureStackView) {
        return azureVirtualMachineTypeProvider.getVmTypes(azureStackView);
    }

    public boolean isSupportedForVm(String vmType) {
        LOGGER.trace("Validating vm type: " + vmType);
        return getSupportedVmTypes().stream().anyMatch(type -> type.equalsIgnoreCase(vmType));
    }

    private Set<String> getSupportedVmTypes() {
        try {
            return JsonUtil.readValue(FileReaderUtils.readFileFromClasspath(AZURE_ACCELERATED_NETWORK_SUPPORT_JSON), new TypeReference<>() {
            });
        } catch (IOException e) {
            LOGGER.error("Failed to read file from location: " + AZURE_ACCELERATED_NETWORK_SUPPORT_JSON, e);
        }
        return Collections.emptySet();
    }
}
