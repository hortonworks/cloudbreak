package com.sequenceiq.environment.parameters.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.GcpParameters;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentParametersConverterTest {
    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_NAME = "envName";

    private static final String ENCRYPTION_KEY = "dummy-encryption-key";

    private static final EnvironmentView ENVIRONMENT_VIEW = new EnvironmentView();

    private static final long ID = 10L;

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @InjectMocks
    private GcpEnvironmentParametersConverter underTest;

    @Test
    void createInstance() {
        assertEquals(GcpParameters.class, underTest.createInstance().getClass());
    }

    @Test
    void getCloudPlatform() {
        assertEquals(CloudPlatform.GCP, underTest.getCloudPlatform());
    }

    @Test
    void convertTest() {
        when(environmentViewConverter.convert(any(Environment.class))).thenReturn(ENVIRONMENT_VIEW);
        ParametersDto parameters = ParametersDto.builder()
                .withId(ID)
                .withGcpParameters(GcpParametersDto.builder()
                        .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                .withEncryptionKey(ENCRYPTION_KEY)
                                .build())
                        .build())
                .build();
        Environment environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setAccountId(ACCOUNT_ID);

        BaseParameters result = underTest.convert(environment, parameters);

        assertEquals(GcpParameters.class, result.getClass());
        GcpParameters gcpResult = (GcpParameters) result;
        assertEquals(ENV_NAME, gcpResult.getName());
        assertEquals(ACCOUNT_ID, gcpResult.getAccountId());
        assertEquals(ENVIRONMENT_VIEW, gcpResult.getEnvironment());
        assertEquals(ID, gcpResult.getId());
        assertEquals(ENCRYPTION_KEY, gcpResult.getEncryptionKey());
    }

    @Test
    void convertToDtoTest() {
        EnvironmentView environmentView = ENVIRONMENT_VIEW;
        GcpParameters parameters = new GcpParameters();
        parameters.setAccountId(ACCOUNT_ID);
        parameters.setEnvironment(environmentView);
        parameters.setId(ID);
        parameters.setName(ENV_NAME);
        parameters.setEncryptionKey(ENCRYPTION_KEY);

        ParametersDto result = underTest.convertToDto(parameters);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(ID, result.getId());
        assertEquals(ENV_NAME, result.getName());
        assertEquals(ENCRYPTION_KEY, result.getGcpParametersDto().getGcpResourceEncryptionParametersDto().getEncryptionKey());
    }
}