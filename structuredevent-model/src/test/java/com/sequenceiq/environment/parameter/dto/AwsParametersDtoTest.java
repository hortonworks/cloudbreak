package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class AwsParametersDtoTest {
    @Test
    void testAwsParametersDtoWithEncryptionParameters() {
        AwsParametersDto dummyAwsParametersDto = AwsParametersDto.builder()
                .withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.builder()
                        .withEncryptionKeyArn("dummy-key-arn")
                        .build())
                .build();
        assertNotNull(dummyAwsParametersDto.getAwsDiskEncryptionParametersDto());
    }

}

