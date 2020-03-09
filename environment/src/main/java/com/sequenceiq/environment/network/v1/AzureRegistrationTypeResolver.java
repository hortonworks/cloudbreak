package com.sequenceiq.environment.network.v1;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AzureRegistrationTypeResolver {

    public RegistrationType getRegistrationType(NetworkDto networkDto) {
        if (networkDto.getAzure() != null && networkDto.getAzure().getNetworkId() != null && networkDto.getAzure().getResourceGroupName() != null) {
            return RegistrationType.EXISTING;
        } else {
            return RegistrationType.CREATE_NEW;
        }
    }

    public RegistrationType getRegistrationType(EnvironmentNetworkRequest networkRequest) {
        EnvironmentNetworkAzureParams azureNetwork = networkRequest.getAzure();
        if (azureNetwork != null && azureNetwork.getNetworkId() != null && azureNetwork.getResourceGroupName() != null) {
            return RegistrationType.EXISTING;
        } else {
            return RegistrationType.CREATE_NEW;
        }
    }
}
