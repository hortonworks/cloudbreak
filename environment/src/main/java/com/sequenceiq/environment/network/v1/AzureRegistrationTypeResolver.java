package com.sequenceiq.environment.network.v1;

import org.springframework.stereotype.Component;

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

}
