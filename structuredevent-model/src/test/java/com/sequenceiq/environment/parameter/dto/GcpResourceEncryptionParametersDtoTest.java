package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcpResourceEncryptionParametersDtoTest {

    private static final String ENCRYPTION_KEY = "dummy-encryption-key";

    @Test
    void testGcoResourceEncryptionParametersDtoWithEncryptionKey() {
        GcpResourceEncryptionParametersDto dummyGcpResourceEncryptionParametersDto = createGcpResourceEncryptionParametersDto();
        assertEquals(dummyGcpResourceEncryptionParametersDto.getEncryptionKey(), ENCRYPTION_KEY);
    }

    private GcpResourceEncryptionParametersDto createGcpResourceEncryptionParametersDto() {
        return GcpResourceEncryptionParametersDto.builder()
                .withEncryptionKey(ENCRYPTION_KEY)
                .build();
    }
}