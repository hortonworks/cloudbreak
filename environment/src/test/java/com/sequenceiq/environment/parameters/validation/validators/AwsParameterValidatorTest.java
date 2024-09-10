package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.parameters.validation.validators.parameter.AwsParameterValidator;

@ExtendWith(MockitoExtension.class)
class AwsParameterValidatorTest {

    private static final Long ENV_ID = 1L;

    @Mock
    private static Credential credential;

    @Mock
    private ParametersService parametersService;

    @InjectMocks
    private AwsParameterValidator underTest;

    private EnvironmentValidationDto environmentValidationDto;

    @BeforeEach
    void setUp() {
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENV_ID);
        environmentDto.setLocation(LocationDto.builder()
                .withName("location")
                .withDisplayName("location")
                .withLatitude(1.0)
                .withLongitude(1.0)
                .build());
        environmentDto.setCredential(credential);
        environmentValidationDto = new EnvironmentValidationDto();
        environmentValidationDto.setValidationType(ValidationType.ENVIRONMENT_CREATION);
        environmentValidationDto.setEnvironmentDto(environmentDto);
    }

    @Test
    void validateAndDetermineAwsParametersAttached() {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .build();

        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParametersDto(awsParameters)
                .build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    void validateNoS3GuardCheckOnUpdateAwsDiskEncryptionParameters() {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .build();
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParametersDto(awsParameters)
                .build();
        EnvironmentDto environmentDto = new AwsParameterValidatorTest.EnvironmentDtoBuilder()
                .withAwsParameters(AwsParametersDto.builder()
                        .build())
                .build();
        environmentValidationDto.setEnvironmentDto(environmentDto);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());
        assertFalse(validationResult.hasError());
    }

    @Test
    void validateWhenAWSDiskEncryptionParametersAlreadyPresent() {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .build();

        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParametersDto(awsParameters)
                .build();

        EnvironmentDto environmentDto = new AwsParameterValidatorTest.EnvironmentDtoBuilder()
                .withAwsParameters(AwsParametersDto.builder()
                        .withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.builder()
                                .withEncryptionKeyArn("dummy-key-arn")
                                .build())
                        .build())
                .build();
        environmentValidationDto.setEnvironmentDto(environmentDto);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testNoEncryptionKeyArnAndEntitlementEnabledThenNoError() {
        EnvironmentDto environmentDto = new AwsParameterValidatorTest.EnvironmentDtoBuilder()
                .withAwsParameters(AwsParametersDto.builder().build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());

        assertFalse(validationResult.hasError());
    }

    @ParameterizedTest(name = "FreeIpa Spot percentage {0} is validated as {1}")
    @MethodSource("freeIpaSpotPercentageParameters")
    void validateFreeIpaSpotPercentage(int percentage, boolean hasError) {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .withFreeIpaSpotPercentage(percentage)
                .build();
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParametersDto(awsParameters)
                .build();

        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());

        assertEquals(hasError, validationResult.hasError());
        if (hasError) {
            assertEquals("FreeIpa spot percentage must be between 0 and 100.", validationResult.getErrors().get(0));
        }
    }

    private static Stream<Arguments> freeIpaSpotPercentageParameters() {
        return Stream.of(
                Arguments.of(-1, true),
                Arguments.of(0, false),
                Arguments.of(50, false),
                Arguments.of(100, false),
                Arguments.of(101, true)
        );
    }

    private static class EnvironmentDtoBuilder {

        private static final String ACCOUNT_ID = "accountId";

        private static final String REGION = "dummyRegion";

        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final ParametersDto.Builder parametersDtoBuilder = ParametersDto.builder();

        public AwsParameterValidatorTest.EnvironmentDtoBuilder withAwsParameters(AwsParametersDto awsParametersDto) {
            parametersDtoBuilder.withAwsParametersDto(awsParametersDto);
            return this;
        }

        public EnvironmentDto build() {
            ParametersDto parametersDto = parametersDtoBuilder.build();
            environmentDto.setParameters(parametersDto);
            environmentDto.setAccountId(ACCOUNT_ID);
            environmentDto.setCredential(credential);
            environmentDto.setLocation(LocationDto.builder().withName(REGION).build());
            return environmentDto;
        }
    }
}

