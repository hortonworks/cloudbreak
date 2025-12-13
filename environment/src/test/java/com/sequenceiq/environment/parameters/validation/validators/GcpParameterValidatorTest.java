package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
    }

    @Test
    public void testWhenNoGcpParametersThenNoError() {
        EnvironmentDto environmentDto = new EnvironmentDtoBuilder().build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    private static class EnvironmentDtoBuilder {

        private static final String ACCOUNT_ID = "accountId";

        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final Builder parametersDtoBuilder = ParametersDto.builder();

        public EnvironmentDtoBuilder withGcpParameters(GcpParametersDto gcpParametersDto) {
            parametersDtoBuilder.withGcpParametersDto(gcpParametersDto);
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
