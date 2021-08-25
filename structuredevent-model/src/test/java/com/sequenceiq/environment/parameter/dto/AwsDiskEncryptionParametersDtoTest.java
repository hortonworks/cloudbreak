package com.sequenceiq.environment.parameter.dto;



import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AwsDiskEncryptionParametersDtoTest {

    @Test
    void testAwsDiskEncryptionParametersDtowithEncryptionKeyArn() {
        AwsDiskEncryptionParametersDto dummyAwsDiskEncryptionParametersDto = createAwsDiskEncryptionParametersDto();
        assertEquals(dummyAwsDiskEncryptionParametersDto.getEncryptionKeyArn(), "dummy-key-arn");
    }

    private AwsDiskEncryptionParametersDto createAwsDiskEncryptionParametersDto() {
        return AwsDiskEncryptionParametersDto.builder().withEncryptionKeyArn("dummy-key-arn").build();
    }
}




