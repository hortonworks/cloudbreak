package com.sequenceiq.environment.environment.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.parameters.validation.validators.AwsParameterProcessor;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidatorServiceTest {

    @Mock
    private EnvironmentRegionValidator regionValidator;

    @Mock
    private EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    @Mock
    private AwsParameterProcessor awsParameterProcessor;

    @InjectMocks
    private EnvironmentValidatorService underTest;

    @Test
    void testValidateAwsEnvironmentDtoNotAWS() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setCloudPlatform("AZURE");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(environmentDto);
        assertTrue(result.hasError());
        assertEquals("Environment is not in AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentDtoValid() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setCloudPlatform("AWS");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(environmentDto);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateAwsEnvironmentRequestNotAWS() {
        EnvironmentRequest request = new EnvironmentRequest();
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AZURE");
        assertTrue(result.hasError());
        assertEquals("Environment request is not for AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoAwsParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentNoS3GuardParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setAws(new AwsEnvironmentParameters());
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoDynamoTable() {
        EnvironmentRequest request = new EnvironmentRequest();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        aws.setS3guard(new S3GuardRequestParameters());
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestValid() {
        EnvironmentRequest request = new EnvironmentRequest();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("table");
        aws.setS3guard(s3GuardRequestParameters);
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertFalse(result.hasError());
    }

}
