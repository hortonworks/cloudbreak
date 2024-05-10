package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;

@Component
public class AzureHostEncryptionValidator {

    public boolean isVmSupported(String vmType, Map<String, AzureVmCapabilities> azureVmCapabilities) {
        return azureVmCapabilities.getOrDefault(vmType, new AzureVmCapabilities(vmType, List.of())).isEncryptionAtHostSupported();
    }
}
