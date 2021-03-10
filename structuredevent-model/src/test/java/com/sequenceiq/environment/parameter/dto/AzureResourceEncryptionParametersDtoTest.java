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

    private AzureResourceEncryptionParametersDto createAzureResourceEncryptionParametersDto() {
        return AzureResourceEncryptionParametersDto.builder().withEncryptionKeyUrl("dummy-key-url").build();
    }
}
