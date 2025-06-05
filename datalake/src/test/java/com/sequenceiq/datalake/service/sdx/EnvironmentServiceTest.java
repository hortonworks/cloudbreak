package com.sequenceiq.datalake.service.sdx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterView;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxClusterViewRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxClusterViewRepository sdxClusterViewRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    void testWaitEnvironmentNetworkCreationFinished() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentEndpoint.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));

        PollingConfig pollingConfig = new PollingConfig(100, TimeUnit.MILLISECONDS, 10, TimeUnit.SECONDS);

        DetailedEnvironmentResponse environment = underTest.waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isNetworkCreationFinished);

        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));
        verify(sdxClusterRepository).findById(sdxId);
        verifyNoMoreInteractions(sdxClusterRepository);
        verify(environmentEndpoint, times(3)).getByCrn("crn");
        verifyNoMoreInteractions(environmentEndpoint);
    }

    @Test
    void testWaitEnvironmentAvailable() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentEndpoint.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.AVAILABLE));

        PollingConfig pollingConfig = new PollingConfig(100, TimeUnit.MILLISECONDS, 10, TimeUnit.SECONDS);

        DetailedEnvironmentResponse environment = underTest.waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isAvailable);

        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.AVAILABLE));
        verify(sdxClusterRepository).findById(sdxId);
        verifyNoMoreInteractions(sdxClusterRepository);
        verify(environmentEndpoint, times(5)).getByCrn("crn");
        verifyNoMoreInteractions(environmentEndpoint);
    }

    private SdxClusterView getClusterView(String envCrn, String envName) {
        SdxClusterView clusterView = new SdxClusterView();
        clusterView.setEnvCrn(envCrn);
        clusterView.setEnvName(envName);
        return clusterView;
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponseWithStatus(EnvironmentStatus status) {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        env.setEnvironmentStatus(status);
        return env;
    }
}
