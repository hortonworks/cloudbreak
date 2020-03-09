package com.sequenceiq.environment.network.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

class AzureRegistrationTypeResolverTest {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver = new AzureRegistrationTypeResolver();

    @Test
    void testNetworkDtoGetRegistrationTypeExisting() {
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(AzureParams.builder()
                        .withNetworkId("NetworkId")
                        .withResourceGroupName("ResourceGroupName")
                        .build())
                .build();
        RegistrationType registrationType = azureRegistrationTypeResolver.getRegistrationType(networkDto);
        Assertions.assertEquals(RegistrationType.EXISTING, registrationType);
    }
}