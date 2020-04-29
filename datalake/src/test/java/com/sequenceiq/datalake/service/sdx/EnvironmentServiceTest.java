package com.sequenceiq.datalake.service.sdx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceTest {

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    public void testWaitEnvironmentNetworkCreationFinished() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentClientService.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));

        DetailedEnvironmentResponse environment = underTest.waitNetworkAndGetEnvironment(sdxId);
        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));
        verifyZeroInteractions(sdxClusterRepository);
        verifyZeroInteractions(environmentClientService);
    }

    @Test
    public void testWaitEnvironmentAvailable() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentClientService.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.AVAILABLE));

        DetailedEnvironmentResponse environment = underTest.waitAndGetEnvironment(sdxId);
        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.AVAILABLE));
        verifyZeroInteractions(sdxClusterRepository);
        verifyZeroInteractions(environmentClientService);
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponseWithStatus(EnvironmentStatus status) {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        env.setEnvironmentStatus(status);
        return env;
    }
}
