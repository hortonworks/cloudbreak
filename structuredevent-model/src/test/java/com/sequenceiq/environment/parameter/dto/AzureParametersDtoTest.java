package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AzureParametersDtoTest {

    @Test
    void testAzureParametersDtowithEncryptionParameters() {
        AzureParametersDto dummyAzureParametersDto = createAzureParametersDto();
        assertEquals(dummyAzureParametersDto.getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl(), "dummy-key-url");
    }

    private AzureParametersDto createAzureParametersDto() {
        return AzureParametersDto.builder()
                        .withEncryptionParameters(
                                AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl("dummy-key-url")
                                        .build())
                        .build();
    }

}
