package com.sequenceiq.environment.parameters.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.BaseParametersRepository;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.notification.domain.DistributionList;

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
    void updateDistributionListDetailsDoesNothingWhenParametersMissing() {
        when(baseParametersRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        DistributionList dl = new DistributionList();
        dl.setResourceName("envName");
        dl.setResourceCrn("crn:test:env");
        dl.setExternalId("distId");

        underTest.updateDistributionListDetails(ENVIRONMENT_ID, dl);

        verify(baseParametersRepository).findByEnvironmentId(ENVIRONMENT_ID);
        verify(baseParametersRepository, never()).save(any());
    }

    @Test
    void updateDistributionListDetailsUpdatesDistributionListAndSaves() {
        BaseParameters params = new AwsParameters();
        params.setDistributionList("oldValue");
        when(baseParametersRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(params));
        when(baseParametersRepository.save(params)).thenReturn(params);

        DistributionList dl = new DistributionList();
        dl.setResourceName("envName");
        dl.setResourceCrn("crn:test:env");
        dl.setExternalId("distId");
        String expectedUuid = "envName/crn:test:env/distId";

        underTest.updateDistributionListDetails(ENVIRONMENT_ID, dl);

        assertEquals(expectedUuid, params.getDistributionList());
        verify(baseParametersRepository).findByEnvironmentId(ENVIRONMENT_ID);
        verify(baseParametersRepository).save(params);
    }
}
