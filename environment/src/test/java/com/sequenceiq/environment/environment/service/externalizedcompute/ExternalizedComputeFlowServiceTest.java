package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeFlowServiceTest {

    public static final String USER_CRN = "userCrn";

    @Mock
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private EnvironmentReactorFlowManager environmentReactorFlowManager;

    @InjectMocks
    private ExternalizedComputeFlowService externalizedComputeFlowService;

    @Test
    public void testReinitializeDefaultExternalizedComputeCluster() {
        Environment environment = mock(Environment.class);
        ExternalizedComputeClusterDto externalizedComputeClusterDto = mock(ExternalizedComputeClusterDto.class);
        FlowIdentifier flowIdentifierMock = mock(FlowIdentifier.class);
        when(environmentReactorFlowManager.triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true)).thenReturn(flowIdentifierMock);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.reinitializeDefaultExternalizedComputeCluster(environment, externalizedComputeClusterDto, true));
        verify(externalizedComputeService, times(1)).checkDefaultClusterExists(environment);
        verify(externalizedComputeService, times(1)).updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        verify(environmentReactorFlowManager, times(1)).triggerExternalizedComputeReinitializationFlow(USER_CRN, environment,
            true);
        assertEquals(flowIdentifierMock, flowIdentifier);
    }

    @Test
    public void testCreateDefaultExternalizedComputeClusterForExistingEnv() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        environment.setName("env");
        String outboundType = "udr";
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .build();

        when(externalizedComputeService.getDefaultCluster(environment)).thenReturn(Optional.empty());
        when(environmentReactorFlowManager.triggerExternalizedComputeClusterCreationFlow(USER_CRN, environment))
                .thenReturn(mock(FlowIdentifier.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        verify(externalizedComputeService, times(1)).updateDefaultComputeClusterProperties(environment, request);
        verify(environmentReactorFlowManager, times(1)).triggerExternalizedComputeClusterCreationFlow(USER_CRN, environment);
    }

    @Test
    public void testCreateDefaultExternalizedComputeClusterForExistingEnvButDefaultClusterExists() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        environment.setName("env");
        String outboundType = "udr";
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .build();

        ExternalizedComputeClusterResponse externalizedComputeClusterResponse = new ExternalizedComputeClusterResponse();
        externalizedComputeClusterResponse.setName("default-env-compute-cluster");

        when(externalizedComputeService.getDefaultCluster(environment)).thenReturn(Optional.of(externalizedComputeClusterResponse));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        assertEquals("You can only have one default externalized compute cluster for an environment", badRequestException.getMessage());

        verify(externalizedComputeService, times(0)).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, times(0)).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }

    @Test
    public void testCreateDefaultExternalizedComputeClusterForExistingEnvButEnvNotAvailable() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.CREATE_FAILED);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        environment.setName("env");
        String outboundType = "udr";
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .build();

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        assertEquals("Environment is not in AVAILABLE state", badRequestException.getMessage());

        verify(externalizedComputeService, times(0)).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, times(0)).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }
}