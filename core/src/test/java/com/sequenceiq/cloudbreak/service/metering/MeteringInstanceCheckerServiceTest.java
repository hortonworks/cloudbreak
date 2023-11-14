package com.sequenceiq.cloudbreak.service.metering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class MeteringInstanceCheckerServiceTest {

    private static final String SALT_RESPONSE = "{\"instance-type\": \"large\"}";

    private static final String DISABLED_SALT_RESPONSE = "";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudContextProvider cloudContextProvider;

    @Mock
    private MismatchedInstanceHandlerService mismatchedInstanceHandlerService;

    @InjectMocks
    private MeteringInstanceCheckerService underTest;

    @Test
    void checkInstanceTypesWithSaltSuccessfully() throws CloudbreakOrchestratorFailedException, IOException {
        StackDto stack = mock(StackDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        prepareStackMocks(stack, instanceGroup, "large");
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(stack);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(instanceGroup, times(1)).getTemplate();
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors);
        verify(mismatchedInstanceHandlerService, times(1)).handleMismatchingInstanceTypes(eq(stack), eq(Set.of()));
    }

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWithMismatchingInstanceTypes() throws CloudbreakOrchestratorFailedException, IOException {
        StackDto stack = mock(StackDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        when(instanceGroup.getGroupName()).thenReturn("master");
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        prepareStackMocks(stack, instanceGroup, "medium");
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(stack);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(instanceGroup, times(1)).getTemplate();
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors);
        verify(mismatchedInstanceHandlerService, times(1)).handleMismatchingInstanceTypes(eq(stack),
                eq(Set.of(new MismatchingInstanceGroup("master", "medium",
                        Map.of("instanceId1", "large", "instanceId2", "large")))));
    }

    @Test
    void checkInstanceTypesWithProviderWhenSaltReturnedEmptyResponse() throws IOException, CloudbreakOrchestratorFailedException {
        StackDto stack = mock(StackDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepareStackMocks(stack, instanceGroup, "large");
        prepeareProviderMocks(stack, metadataCollector);
        JsonNode saltResponse = JsonUtil.readTree(DISABLED_SALT_RESPONSE);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(stack);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(), eq(List.of("instanceId1", "instanceId2")));
        verify(instanceGroup, times(1)).getTemplate();
        verify(mismatchedInstanceHandlerService, times(1)).handleMismatchingInstanceTypes(eq(stack), eq(Set.of()));
    }

    @Test
    void checkInstanceTypesWithProviderWhenSaltFailed() throws IOException, CloudbreakOrchestratorFailedException {
        StackDto stack = mock(StackDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepareStackMocks(stack, instanceGroup, "large");
        prepeareProviderMocks(stack, metadataCollector);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenThrow(new CloudbreakOrchestratorFailedException("failed"));

        underTest.checkInstanceTypes(stack);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(), eq(List.of("instanceId1", "instanceId2")));
        verify(instanceGroup, times(1)).getTemplate();
        verify(mismatchedInstanceHandlerService, times(1)).handleMismatchingInstanceTypes(eq(stack), eq(Set.of()));
    }

    private void prepeareProviderMocks(StackDto stack, MetadataCollector metadataCollector) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(metadataCollector.collectInstanceTypes(eq(authenticatedContext), eq(List.of("instanceId1", "instanceId2"))))
                .thenReturn(new InstanceTypeMetadata(Map.of("instanceId1", "large", "instanceId2", "large")));
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudContextProvider.getCloudContext(eq(stack))).thenReturn(CloudContext.Builder.builder().build());
    }

    private void prepareStackMocks(StackDto stack, InstanceGroupView instanceGroup, String instanceType) {
        InstanceMetadataView instance1 = mock(InstanceMetadataView.class);
        lenient().when(instance1.getDiscoveryFQDN()).thenReturn("host1");
        when(instance1.getInstanceId()).thenReturn("instanceId1");
        InstanceMetadataView instance2 = mock(InstanceMetadataView.class);
        lenient().when(instance2.getDiscoveryFQDN()).thenReturn("host2");
        when(instance2.getInstanceId()).thenReturn("instanceId2");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(instance1, instance2));
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup, List.of(instance1, instance2))));
        when(gatewayConfigService.getPrimaryGatewayConfig(eq(stack))).thenReturn(new GatewayConfig(null, null, null, null, null, null));

        Template template = new Template();
        template.setInstanceType(instanceType);
        when(instanceGroup.getTemplate()).thenReturn(template);
    }

}