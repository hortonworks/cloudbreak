package com.sequenceiq.environment.network.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(RegistrationType.EXISTING, registrationType);
    }
}