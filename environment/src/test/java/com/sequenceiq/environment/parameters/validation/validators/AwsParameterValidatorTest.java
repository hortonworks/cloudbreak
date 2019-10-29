package com.sequenceiq.environment.parameters.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.service.NoSqlTableCreationModeDeterminerService;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@ExtendWith(MockitoExtension.class)
class AwsParameterValidatorTest {

    @Mock
    private NoSqlTableCreationModeDeterminerService noSqlTableCreationModeDeterminerService;

    @Mock
    private ParametersService parametersService;

    @InjectMocks
    private AwsParameterValidator underTest;

    private EnvironmentDto environmentDto;

    @BeforeEach
    void setUp() {
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        environmentDto = new EnvironmentDto();
        environmentDto.setLocation(new LocationDto("location", "location", 1.0, 1.0));
        environmentDto.setCredential(credential);
    }

    @Test
    void validateAndDetermineAwsParametersAttached() {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .withDynamoDbTableName("tablename")
                .build();
        when(parametersService.isS3GuardTableUsed(any(), any(), any(), any())).thenReturn(true);
        ValidationResult validationResult = underTest.validateAndDetermineAwsParameters(environmentDto, awsParameters);
        assertTrue(validationResult.hasError());
        assertEquals(1L, validationResult.getErrors().size());
        assertEquals("S3Guard table 'tablename' is already attached to another active environment. "
                + "Please select another unattached table or specify a non-existing name to create it.", validationResult.getErrors().get(0));
        verify(noSqlTableCreationModeDeterminerService, never()).determineCreationMode(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = S3GuardTableCreation.class, names = {"USE_EXISTING", "CREATE_NEW"})
    void validateAndDetermineAwsParametersUseExisting(S3GuardTableCreation creation) {
        AwsParametersDto awsParameters = AwsParametersDto.builder()
                .withDynamoDbTableName("tablename")
                .build();
        when(parametersService.isS3GuardTableUsed(any(), any(), any(), any())).thenReturn(false);
        when(noSqlTableCreationModeDeterminerService.determineCreationMode(any(), any())).thenReturn(creation);
        ValidationResult validationResult = underTest.validateAndDetermineAwsParameters(environmentDto, awsParameters);
        assertFalse(validationResult.hasError());
        verify(noSqlTableCreationModeDeterminerService).determineCreationMode(any(), any());
        assertEquals(creation, awsParameters.getDynamoDbTableCreation());
    }
}
