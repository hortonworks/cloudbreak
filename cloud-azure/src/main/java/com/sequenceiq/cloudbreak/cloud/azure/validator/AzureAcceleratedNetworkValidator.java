package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.azure.util.AzureVirtualMachineTypeProvider;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import javax.inject.Inject;

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

    private boolean isSupportedForVm(String vmType) {
        LOGGER.debug("Validating vm type: " + vmType);
        String[] parts = vmType.split("_");
        String transformedFlavor = "";
        boolean validCpu = false;
        if (parts.length > 1) {
            String segment = parts[1];
            validCpu = isValidCpu(segment);
            String transformedSegment = getSegment(segment);
            transformedFlavor = vmType.replaceAll(segment, transformedSegment).toLowerCase();
            LOGGER.debug("Transformed flavor to validate: " + transformedFlavor);
        }
        return validCpu && getSupportedVmTypes().stream().parallel().anyMatch(transformedFlavor::endsWith);
    }

    private String getSegment(String segment) {
        return segment
                .replaceAll("[0-9]", "")
                .replaceAll("-", "")
                .toLowerCase();
    }

    private boolean isValidCpu(String segment) {
        Matcher matcher = Pattern.compile("\\d+").matcher(segment);
        if (matcher.find()) {
            int cpuCoreSize = Integer.parseInt(matcher.group());
            if (cpuCoreSize > 2) {
                return true;
            } else {
                LOGGER.debug("Core number must be greater than 2.");
                return false;
            }
        } else {
            LOGGER.debug("Core number not found in the instance name.");
            return false;
        }
    }

    private Set<String> getSupportedVmTypes() {
        try {
            return JsonUtil.readValue(FileReaderUtils.readFileFromClasspath(AZURE_ACCELERATED_NETWORK_SUPPORT_JSON), new TypeReference<Set<String>>() { });
        } catch (IOException e) {
            LOGGER.error("Failed to read file from location: " + AZURE_ACCELERATED_NETWORK_SUPPORT_JSON, e);
        }
        return Collections.emptySet();
    }
}
