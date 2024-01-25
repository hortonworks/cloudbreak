package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AzureHostEncryptionValidator {

    public boolean isVmSupported(String vmType, Map<String, Boolean> hostEncryptionSupport) {
        boolean supported = false;
        if (hostEncryptionSupport != null) {
            Boolean vmIsSupported = hostEncryptionSupport.get(vmType);
            if (vmIsSupported != null && vmIsSupported) {
                supported = true;
            }
        }
        return supported;
    }
}
