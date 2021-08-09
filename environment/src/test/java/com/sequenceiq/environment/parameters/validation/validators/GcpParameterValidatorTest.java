package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto.Builder;
import com.sequenceiq.environment.parameters.validation.validators.parameter.GcpParameterValidator;

public class GcpParameterValidatorTest {

    private static final String ENCRYPTION_KEY = "encryptionKey";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private GcpParameterValidator underTest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testWhenNoGcpParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder().build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenFeatureTurnedOffAndEncryptionKeyProvidedThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withGcpParameters(GcpParametersDto.builder()
                        .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                .withEncryptionKey(ENCRYPTION_KEY)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(anyString())).thenReturn(false);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertTrue(validationResult.hasError());
    }

    @Test
    public void testWhenFeatureTurnedONAndEncryptionKeyNotProvidedThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withGcpParameters(GcpParametersDto.builder()
                        .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder().build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(anyString())).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWhenFeatureTurnedONAndEncryptionKeyProvidedThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder()
                .withGcpParameters(GcpParametersDto.builder()
                        .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                .withEncryptionKey(ENCRYPTION_KEY)
                                .build())
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(anyString())).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    private static class EnvironmentDtoBuilder {

        private static final String ACCOUNT_ID = "accountId";

        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final Builder parametersDtoBuilder = ParametersDto.builder();

        public EnvironmentDtoBuilder withGcpParameters(GcpParametersDto gcpParametersDto) {
            parametersDtoBuilder.withGcpParameters(gcpParametersDto);
            return this;
        }

        public EnvironmentDto build() {
            ParametersDto parametersDto = parametersDtoBuilder.build();
            environmentDto.setParameters(parametersDto);
            environmentDto.setAccountId(ACCOUNT_ID);
            return environmentDto;
        }
    }
}