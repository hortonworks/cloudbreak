package com.sequenceiq.environment.parameters.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@ExtendWith(MockitoExtension.class)
class AwsEnvironmentParametersConverterTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_NAME = "envName";

    private static final String TABLE_NAME = "dynamotable";

    private static final EnvironmentView ENVIRONMENT_VIEW = new EnvironmentView();

    private static final long ID = 10L;

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @InjectMocks
    private AwsEnvironmentParametersConverter underTest;

    @Test
    void createInstance() {
        assertEquals(AwsParameters.class, underTest.createInstance().getClass());
    }

    @Test
    void getCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    void convertTest() {
        when(environmentViewConverter.convert(any(Environment.class))).thenReturn(ENVIRONMENT_VIEW);
        ParametersDto parameters = ParametersDto.builder()
                .withId(ID)
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName(TABLE_NAME)
                        .withDynamoDbTableCreation(S3GuardTableCreation.CREATE_NEW)
                        .build())
                .build();
        Environment environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setAccountId(ACCOUNT_ID);

        BaseParameters result = underTest.convert(environment, parameters);

        assertEquals(AwsParameters.class, result.getClass());
        AwsParameters awsResult = (AwsParameters) result;
        assertEquals(ENV_NAME, awsResult.getName());
        assertEquals(ACCOUNT_ID, awsResult.getAccountId());
        assertEquals(ENVIRONMENT_VIEW, awsResult.getEnvironment());
        assertEquals(ID, awsResult.getId());
        assertEquals(TABLE_NAME, awsResult.getS3guardTableName());
        assertEquals(S3GuardTableCreation.CREATE_NEW, awsResult.getS3guardTableCreation());
    }

    @Test
    void convertToDtoTest() {
        EnvironmentView environmentView = ENVIRONMENT_VIEW;
        AwsParameters parameters = new AwsParameters();
        parameters.setAccountId(ACCOUNT_ID);
        parameters.setEnvironment(environmentView);
        parameters.setId(ID);
        parameters.setName(ENV_NAME);
        parameters.setS3guardTableName(TABLE_NAME);
        parameters.setS3guardTableCreation(S3GuardTableCreation.CREATE_NEW);

        ParametersDto result = underTest.convertToDto(parameters);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(ID, result.getId());
        assertEquals(ENV_NAME, result.getName());
        assertEquals(TABLE_NAME, result.getAwsParametersDto().getS3GuardTableName());
        assertEquals(S3GuardTableCreation.CREATE_NEW, result.getAwsParametersDto().getDynamoDbTableCreation());
    }
}
