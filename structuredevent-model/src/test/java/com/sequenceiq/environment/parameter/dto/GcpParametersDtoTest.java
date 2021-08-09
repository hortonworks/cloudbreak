package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcpParametersDtoTest {

    private static final String ENCRYPTION_KEY = "dummy-encryption-key";

    @Test
    void testGcpParametersDtoWithEncryptionParametersWithEncryptionKey() {
        GcpParametersDto dummyGcpParametersDto = GcpParametersDto.builder()
                .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                        .withEncryptionKey(ENCRYPTION_KEY)
                        .build())
                .build();
        assertEquals(dummyGcpParametersDto.getGcpResourceEncryptionParametersDto().getEncryptionKey(), ENCRYPTION_KEY);
    }
}