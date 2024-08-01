package com.sequenceiq.environment.environment.service.externalizedcompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeFlowServiceTest {

    private static final String USER_CRN = "userCrn";

    @Mock
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private EnvironmentReactorFlowManager environmentReactorFlowManager;

    @Mock
    private EnvironmentValidatorService environmentValidatorService;

    @InjectMocks
    private ExternalizedComputeFlowService externalizedComputeFlowService;

    @Test
    void testReinitializeDefaultExternalizedComputeCluster() {
        Environment environment = new Environment();
        BaseNetwork baseNetwork = mock(BaseNetwork.class);
        Map<String, CloudSubnet> subnetMap = new HashMap<>();
        subnetMap.put("subnet1", new CloudSubnet());
        when(baseNetwork.getSubnetMetas()).thenReturn(subnetMap);
        environment.setNetwork(baseNetwork);
        ExternalizedComputeClusterDto externalizedComputeClusterDto = ExternalizedComputeClusterDto.builder().withCreate(true).build();
        FlowIdentifier flowIdentifierMock = mock(FlowIdentifier.class);
        when(environmentReactorFlowManager.triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true)).thenReturn(flowIdentifierMock);
        when(environmentValidatorService.validateExternalizedComputeCluster(eq(externalizedComputeClusterDto), any(), any()))
                .thenReturn(ValidationResult.builder().build());
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.reinitializeDefaultExternalizedComputeCluster(environment, externalizedComputeClusterDto, true));
        verify(environmentValidatorService, times(1)).validateExternalizedComputeCluster(eq(externalizedComputeClusterDto), any(), any());
        verify(externalizedComputeService, times(1)).checkDefaultCluster(environment, true);
        verify(externalizedComputeService, times(1)).updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        verify(environmentReactorFlowManager, times(1)).triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true);
        assertEquals(flowIdentifierMock, flowIdentifier);
    }

    @Test
    void testReinitializeDefaultExternalizedComputeClusterWhenCreateIsFalse() {
        Environment environment = new Environment();
        ExternalizedComputeClusterDto externalizedComputeClusterDto = ExternalizedComputeClusterDto.builder().withCreate(false).build();
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.reinitializeDefaultExternalizedComputeCluster(environment, externalizedComputeClusterDto, true)));
        assertEquals("Create field is disabled in externalized compute cluster request!", badRequestException.getMessage());
        verify(environmentValidatorService, never()).validateExternalizedComputeCluster(eq(externalizedComputeClusterDto), any(), any());
        verify(externalizedComputeService, never()).checkDefaultCluster(environment, false);
        verify(externalizedComputeService, never()).updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        verify(environmentReactorFlowManager, never()).triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true);
    }

    @Test
    void testReinitializeDefaultExternalizedComputeClusterWhenValidationFailed() {
        Environment environment = new Environment();
        BaseNetwork baseNetwork = mock(BaseNetwork.class);
        Map<String, CloudSubnet> subnetMap = new HashMap<>();
        subnetMap.put("subnet1", new CloudSubnet());
        when(baseNetwork.getSubnetMetas()).thenReturn(subnetMap);
        environment.setNetwork(baseNetwork);
        ExternalizedComputeClusterDto externalizedComputeClusterDto = ExternalizedComputeClusterDto.builder().withCreate(true).build();
        when(environmentValidatorService.validateExternalizedComputeCluster(eq(externalizedComputeClusterDto), any(), any()))
                .thenReturn(ValidationResult.builder().error("error").build());
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.reinitializeDefaultExternalizedComputeCluster(environment, externalizedComputeClusterDto, true)));
        assertEquals("error", badRequestException.getMessage());
        verify(environmentValidatorService, times(1)).validateExternalizedComputeCluster(eq(externalizedComputeClusterDto), any(), any());
        verify(externalizedComputeService, never()).checkDefaultCluster(environment, false);
        verify(externalizedComputeService, never()).updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        verify(environmentReactorFlowManager, never()).triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true);
    }

    @Test
    void testCreateDefaultExternalizedComputeClusterForExistingEnv() {
        Environment environment = new Environment();
        BaseNetwork baseNetwork = mock(BaseNetwork.class);
        Map<String, CloudSubnet> subnetMap = new HashMap<>();
        subnetMap.put("subnet1", new CloudSubnet());
        when(baseNetwork.getSubnetMetas()).thenReturn(subnetMap);
        environment.setNetwork(baseNetwork);
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

        when(environmentValidatorService.validateExternalizedComputeCluster(eq(request), any(), any())).thenReturn(ValidationResult.builder().build());
        when(externalizedComputeService.getDefaultCluster(environment)).thenReturn(Optional.empty());
        when(environmentReactorFlowManager.triggerExternalizedComputeClusterCreationFlow(USER_CRN, environment))
                .thenReturn(mock(FlowIdentifier.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        verify(environmentValidatorService, times(1)).validateExternalizedComputeCluster(eq(request), any(), any());
        verify(externalizedComputeService, times(1)).updateDefaultComputeClusterProperties(environment, request);
        verify(environmentReactorFlowManager, times(1)).triggerExternalizedComputeClusterCreationFlow(USER_CRN, environment);
    }

    @Test
    void testCreateDefaultExternalizedComputeClusterForExistingEnvButDefaultClusterExists() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        environment.setName("env");
        environment.setNetwork(mock(BaseNetwork.class));
        String outboundType = "udr";
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .build();

        ExternalizedComputeClusterResponse externalizedComputeClusterResponse = new ExternalizedComputeClusterResponse();
        externalizedComputeClusterResponse.setName("default-env-compute-cluster");

        when(environmentValidatorService.validateExternalizedComputeCluster(eq(request), any(), any())).thenReturn(ValidationResult.builder().build());
        when(externalizedComputeService.getDefaultCluster(environment)).thenReturn(Optional.of(externalizedComputeClusterResponse));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        assertEquals("You can only have one default externalized compute cluster for an environment", badRequestException.getMessage());

        verify(environmentValidatorService, times(1)).validateExternalizedComputeCluster(eq(request), any(), any());
        verify(externalizedComputeService, times(0)).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, times(0)).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }

    @Test
    void testCreateDefaultExternalizedComputeClusterForExistingEnvButEnvNotAvailable() {
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

        verify(environmentValidatorService, times(0)).validateExternalizedComputeCluster(eq(request), any(), any());
        verify(externalizedComputeService, times(0)).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, times(0)).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }

    @Test
    void testCreateDefaultExternalizedComputeClusterForExistingEnvButCreateIsFalse() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setId(1L);
        environment.setResourceCrn("crn");
        environment.setName("env");
        String outboundType = "udr";
        ExternalizedComputeClusterDto request = ExternalizedComputeClusterDto.builder()
                .withCreate(false)
                .withPrivateCluster(true)
                .withOutboundType(outboundType)
                .build();

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        assertEquals("Create field is disabled in externalized compute cluster request!", badRequestException.getMessage());

        verify(environmentValidatorService, never()).validateExternalizedComputeCluster(eq(request), any(), any());
        verify(externalizedComputeService, never()).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, never()).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }

    @Test
    void testCreateDefaultExternalizedComputeClusterForExistingEnvButValidationFailed() {
        Environment environment = new Environment();
        BaseNetwork baseNetwork = mock(BaseNetwork.class);
        Map<String, CloudSubnet> subnetMap = new HashMap<>();
        subnetMap.put("subnet1", new CloudSubnet());
        when(baseNetwork.getSubnetMetas()).thenReturn(subnetMap);
        environment.setNetwork(baseNetwork);
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

        when(environmentValidatorService.validateExternalizedComputeCluster(eq(request), any(), any()))
                .thenReturn(ValidationResult.builder().error("error").build());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> externalizedComputeFlowService.createDefaultExternalizedComputeClusterForExistingEnv(environment, request));

        assertEquals("error", badRequestException.getMessage());

        verify(environmentValidatorService, times(1)).validateExternalizedComputeCluster(eq(request), any(), any());
        verify(externalizedComputeService, never()).updateDefaultComputeClusterProperties(any(), any());
        verify(environmentReactorFlowManager, never()).triggerExternalizedComputeClusterCreationFlow(any(), any());
    }
}