package com.sequenceiq.environment.parameter.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AwsParametersDtoTest {
    @Test
    void testAwsParametersDtoWithEncryptionParameters() {
        AwsParametersDto dummyAwsParametersDto = AwsParametersDto.builder()
                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                        .withEncryptionKeyArn("dummy-key-arn")
                .build())
                .build();
        Assertions.assertNotNull(dummyAwsParametersDto.getAwsDiskEncryptionParametersDto());
    }

}

