package com.sequenceiq.environment.parameter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AwsParametersDtoTest {
    @Test
    void testAwsParametersDtowithEncryptionParameters() {
        AwsParametersDto dummyAwsParametersDto = createAwsParametersDto();
        assertEquals(dummyAwsParametersDto.getAwsDiskEncryptionParametersDto().getEncryptionKeyArn(), "dummy-key-arn");
    }

    private AwsParametersDto createAwsParametersDto() {
        return AwsParametersDto.builder()
                .withAwsDiskEncryptionParameters(
                        AwsDiskEncryptionParametersDto.builder()
                                .withEncryptionKeyArn("dummy-key-arn")
                                .build())
                .build();
    }

}

