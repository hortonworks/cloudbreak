package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AzureHostEncryptionValidator {

    public boolean isVmSupported(String vmType, Map<String, Boolean> hostEncryptionSupport) {
        return hostEncryptionSupport.get(vmType);
    }
}
