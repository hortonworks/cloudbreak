package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;

class UpdateAzureResourceEncryptionDtoTest {

    @Test
    void builderTest() {
        UpdateAzureResourceEncryptionDto underTest = new UpdateAzureResourceEncryptionDto();
        underTest.setAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder().build());

        assertThat(underTest.getAzureResourceEncryptionParametersDto()).isNotNull();
    }
}