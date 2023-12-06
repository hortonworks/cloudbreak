package com.sequenceiq.environment.api.v1.environment.endpoint.service.azure;

import org.springframework.stereotype.Service;

import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class HostEncryptionCalculator {

    public boolean hostEncryptionRequired(DetailedEnvironmentResponse environmentResponse) {
        AzureEnvironmentParameters azure = environmentResponse.getAzure();
        if (azure != null) {
            AzureResourceEncryptionParameters encryptionParameters = azure.getResourceEncryptionParameters();
            if (encryptionParameters != null) {
                return encryptionParameters.isEnableHostEncryption();
            }
        }
        return false;
    }
}
