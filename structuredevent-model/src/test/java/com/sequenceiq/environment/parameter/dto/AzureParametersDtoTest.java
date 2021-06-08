package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AzureParametersDtoTest {

    @Test
    void testAzureParametersDtoWithEncryptionParametersWithEncryptionKeyUrlAndEncryptionKeyResourceGroupName() {
        AzureParametersDto dummyAzureParametersDto = AzureParametersDto.builder()
                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummy-key-url")
                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                        .build())
                .build();
        assertEquals(dummyAzureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl(), "dummy-key-url");
        assertEquals(dummyAzureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyResourceGroupName(), "dummyResourceGroupName");
    }

    @Test
    void testAzureParametersDtoWithEncryptionParametersWithEncryptionKeyUrl() {
        AzureParametersDto dummyAzureParametersDto = AzureParametersDto.builder()
                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummy-key-url")
                        .build())
                .build();
        assertEquals(dummyAzureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl(), "dummy-key-url");
        assertNull(dummyAzureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyResourceGroupName());
    }

}
