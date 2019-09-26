package com.sequenceiq.environment.parameters.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.BaseParametersRepository;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;

@ExtendWith(MockitoExtension.class)
class ParametersServiceTest {

    private static final long ENVIRONMENT_ID = 10L;

    @Mock
    private BaseParametersRepository baseParametersRepository;

    @Mock
    private EnvironmentParametersConverter environmentParametersConverter;

    @Mock
    private Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    @InjectMocks
    private ParametersService underTest;

    @Test
    void findByEnvironment() {
        underTest.findByEnvironment(ENVIRONMENT_ID);

        verify(baseParametersRepository).findByEnvironmentId(ENVIRONMENT_ID);
    }

    @Test
    void saveParameters() {
        AwsParameters awsParameters = new AwsParameters();
        when(environmentParamsConverterMap.get(any(CloudPlatform.class))).thenReturn(environmentParametersConverter);
        when(environmentParametersConverter.convert(any(Environment.class), any(ParametersDto.class))).thenReturn(awsParameters);
        when(baseParametersRepository.save(any())).thenReturn(awsParameters);
        Environment environment = new Environment();
        environment.setAccountId("accountId");
        environment.setCloudPlatform("AWS");

        BaseParameters result = underTest.saveParameters(environment, ParametersDto.builder().build());
        assertEquals(awsParameters, result);
    }

    @Test
    void isS3GuardTableUsed() {
        when(baseParametersRepository.isS3GuardTableUsed(any(), any(), any(), any(), any())).thenReturn(true);
        assertTrue(underTest.isS3GuardTableUsed("accountid", "platform", "region", "tablename"));
        when(baseParametersRepository.isS3GuardTableUsed(any(), any(), any(), any(), any())).thenReturn(false);
        assertFalse(underTest.isS3GuardTableUsed("accountid", "platform", "region", "tablename"));
    }
}
