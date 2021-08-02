package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.service.NoSqlTableCreationModeDeterminerService;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.parameter.dto.s3guard.S3GuardTableCreation;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.environment.parameters.validation.validators.parameter.AwsParameterValidator;

@ExtendWith(MockitoExtension.class)
class AwsParameterValidatorTest {

    private static final Long ENV_ID = 1L;

    @Mock
    private NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService;

    @Mock
    private ParametersService parametersService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsParameterValidator underTest;

    private EnvironmentValidationDto environmentValidationDto;

    @BeforeEach
    void setUp() {
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENV_ID);
        environmentDto.setLocation(new LocationDto("location", "location", 1.0, 1.0));
        environmentDto.setCredential(credential);
        environmentValidationDto = new EnvironmentValidationDto();
        environmentValidationDto.setValidationType(ValidationType.ENVIRONMENT_CREATION);
        environmentValidationDto.setEnvironmentDto(environmentDto);
    }

    @Test
    void validateAndDetermineAwsParametersAttached() {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .withDynamoDbTableName("tablename")
                .build();

        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(awsParameters)
                .build();
        when(parametersService.isS3GuardTableUsed(any(), any(), any(), any())).thenReturn(true);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());

        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("S3Guard Dynamo table 'tablename' is already attached to another active environment. "
                + "Please select another unattached table or specify a non-existing name to create it. Refer to "
                + "Cloudera documentation at https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-aws-req-dynamodb.html " +
                "for the required setup.", validationResult.getErrors().get(0));
        verify(noSqlTableCreationModeDeterminerService, never()).determineCreationMode(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = S3GuardTableCreation.class, names = {"USE_EXISTING", "CREATE_NEW"})
    void validateAndDetermineAwsParametersUseExisting(S3GuardTableCreation creation) {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .withDynamoDbTableName("tablename")
                .build();
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(awsParameters)
                .build();
        when(parametersService.isS3GuardTableUsed(any(), any(), any(), any())).thenReturn(false);
        when(noSqlTableCreationModeDeterminerService.determineCreationMode(any(), any())).thenReturn(creation);

        ValidationResult validationResult = underTest.validate(environmentValidationDto, parametersDto, ValidationResult.builder());

        assertFalse(validationResult.hasError());
        verify(noSqlTableCreationModeDeterminerService).determineCreationMode(any(), any());
        assertEquals(creation, awsParameters.getDynamoDbTableCreation());
        verify(parametersService, times(1)).saveParameters(ENV_ID, parametersDto);
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

    @Test
    public void testWhenEncryptionKeyArnPresentAndEntitlementDisabledThenError() {
        EnvironmentDto environmentDto = new AwsParameterValidatorTest.EnvironmentDtoBuilder()
                                        .withAwsParameters(AwsParametersDto.builder()
                                        .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                                            .withEncryptionKeyArn("dummy-key-arn")
                                            .build())
                                            .build())
                                        .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        when(entitlementService.isAWSDiskEncryptionWithCMKEnabled(anyString())).thenReturn(false);
        ValidationResult validationResult = underTest.validate(environmentValidationDto, environmentDto.getParameters(), ValidationResult.builder());
        assertTrue(validationResult.hasError());
    }

    @Test
    public void testWhenEncryptionKeyArnPresentAndEntitlementEnabledThenNoError() {
        EnvironmentDto environmentDto = new AwsParameterValidatorTest.EnvironmentDtoBuilder()
                                            .withAwsParameters(AwsParametersDto.builder()
                                            .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                                                    .withEncryptionKeyArn("dummy-key-arn")
                                                    .build())
                                                    .build())
                                        .build();

        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        when(entitlementService.isAWSDiskEncryptionWithCMKEnabled(anyString())).thenReturn(true);
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
                .withAwsParameters(awsParameters)
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

        private final EnvironmentDto environmentDto = new EnvironmentDto();

        private final ParametersDto.Builder parametersDtoBuilder = ParametersDto.builder();

        public AwsParameterValidatorTest.EnvironmentDtoBuilder withAwsParameters(AwsParametersDto awsParametersDto) {
            parametersDtoBuilder.withAwsParameters(awsParametersDto);
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
