package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AzureResourceEncryptionParametersDtoTest {

    @Test
    void testAzureResourceEncryptionParametersDtowithEncryptionKeyUrl() {
        AzureResourceEncryptionParametersDto dummyAzureResourceEncryptionParametersDto = createAzureResourceEncryptionParametersDto();
        assertEquals(dummyAzureResourceEncryptionParametersDto.getEncryptionKeyUrl(), "dummy-key-url");
    }

    @Test
    void testAzureResourceEncryptionParametersDtowithDiskEncryptionSetId() {
        AzureResourceEncryptionParametersDto dummyAzureResourceEncryptionParametersDto = createAzureResourceEncryptionParametersDto();
        assertEquals(dummyAzureResourceEncryptionParametersDto.getDiskEncryptionSetId(), "dummy-des-id");
    }

    private AzureResourceEncryptionParametersDto createAzureResourceEncryptionParametersDto() {
        return AzureResourceEncryptionParametersDto.builder()
                .withDiskEncryptionSetId("dummy-des-id")
                .withEncryptionKeyUrl("dummy-key-url")
                .build();
    }
}
