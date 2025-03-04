package com.sequenceiq.cloudbreak.service.metering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@ExtendWith(MockitoExtension.class)
class MeteringInstanceCheckerServiceTest {

    private static final String SALT_RESPONSE = "{\"instance-type\": \"large\"}";

    private static final String DISABLED_SALT_RESPONSE = "";

    private static final Long STACK_ID = 1L;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @InjectMocks
    private MeteringInstanceCheckerService underTest;

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWhenNoMismatchFound() throws CloudbreakOrchestratorFailedException, IOException {
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        Stack stack = stack("large", "large", "large");
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors, instanceMetaDataService, cloudbreakEventService);
    }

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWithMismatchingProviderInstanceTypes() throws CloudbreakOrchestratorFailedException, IOException {
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        Stack stack = stack("large", "medium", null);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors, cloudbreakEventService);
        verify(instanceMetaDataService, times(2)).save(any());
    }

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWithMismatchingProviderAndTemplateInstanceTypes() throws CloudbreakOrchestratorFailedException, IOException {
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        Stack stack = stack("small", "medium", null);
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors);
        verify(instanceMetaDataService, times(2)).save(any());
        verify(cloudbreakEventService, times(1)).cloudbreakLastEventsForStack(eq(STACK_ID), anyString(), anyInt());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(any(), eq("PROVIDER_INSTANCES_ARE_DIFFERENT"),
                eq(ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH), any());
    }

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWithMismatchingTemplateInstanceTypes() throws CloudbreakOrchestratorFailedException, IOException {
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        Stack stack = stack("small", "large", "large");
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors, instanceMetaDataService);
        verify(cloudbreakEventService, times(1)).cloudbreakLastEventsForStack(eq(STACK_ID), anyString(), anyInt());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(any(), eq("PROVIDER_INSTANCES_ARE_DIFFERENT"),
                eq(ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH), any());
    }

    @Test
    void checkInstanceTypesWithSaltSuccessfullyWithMismatchingTemplateInstanceTypesWhenLatestEventWasTheSame()
            throws CloudbreakOrchestratorFailedException, IOException {
        JsonNode saltResponse = JsonUtil.readTree(SALT_RESPONSE);
        Stack stack = stack("small", "large", "large");
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType("PROVIDER_INSTANCES_ARE_DIFFERENT");
        when(cloudbreakEventService.cloudbreakLastEventsForStack(eq(STACK_ID), anyString(), anyInt()))
                .thenReturn(List.of(new StructuredNotificationEvent(null, notificationDetails)));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verifyNoInteractions(cloudContextProvider, credentialClientService, cloudPlatformConnectors, instanceMetaDataService);
        verify(cloudbreakEventService, times(1)).cloudbreakLastEventsForStack(eq(STACK_ID), anyString(), anyInt());
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(any(), eq("PROVIDER_INSTANCES_ARE_DIFFERENT"),
                eq(ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH), any());
    }

    @Test
    void checkInstanceTypesWithProviderWhenSaltReturnedEmptyResponse() throws IOException, CloudbreakOrchestratorFailedException {
        Stack stack = stack("large", null, null);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepeareProviderMocks(stack, metadataCollector);
        JsonNode saltResponse = JsonUtil.readTree(DISABLED_SALT_RESPONSE);
        when(gatewayConfigService.getPrimaryGatewayConfig(eq(stack))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenReturn(Map.of("host1", saltResponse, "host2", saltResponse));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(),
                argThat(list -> list.size() == 2 && list.containsAll(Set.of("instanceId1", "instanceId2"))));
        verify(instanceMetaDataService, times(2)).save(any());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void checkInstanceTypesWithProviderWhenSaltFailed() throws IOException, CloudbreakOrchestratorFailedException {
        Stack stack = stack("large", null, null);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepeareProviderMocks(stack, metadataCollector);
        when(gatewayConfigService.getPrimaryGatewayConfig(eq(stack))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenThrow(new CloudbreakOrchestratorFailedException("failed"));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(),
                argThat(list -> list.size() == 2 && list.containsAll(Set.of("instanceId1", "instanceId2"))));
        verify(instanceMetaDataService, times(2)).save(any());
        verifyNoInteractions(cloudbreakEventService);
    }

    @Test
    void checkInstanceTypesWithProviderShouldNotThrowExceptionWhenAuthenticationFailed() throws IOException, CloudbreakOrchestratorFailedException {
        Stack stack = stack("large", null, null);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepeareProviderMocks(stack, metadataCollector);
        when(gatewayConfigService.getPrimaryGatewayConfig(eq(stack))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        when(metadataCollector.collectInstanceTypes(any(), anyList())).thenThrow(new ProviderAuthenticationFailedException("no permission"));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(),
                argThat(list -> list.size() == 2 && list.containsAll(Set.of("instanceId1", "instanceId2"))));
        verifyNoInteractions(instanceMetaDataService, cloudbreakEventService);
    }

    @Test
    void checkInstanceTypesWithProviderShouldNotThrowExceptionWhenAwsAuthenticationFailed() throws IOException, CloudbreakOrchestratorFailedException {
        Stack stack = stack("large", null, null);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        prepeareProviderMocks(stack, metadataCollector);
        when(gatewayConfigService.getPrimaryGatewayConfig(eq(stack))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(hostOrchestrator.getGrainOnAllHosts(any(), anyString())).thenThrow(new CloudbreakOrchestratorFailedException("failed"));
        when(metadataCollector.collectInstanceTypes(any(), anyList())).thenThrow(new RuntimeException("user is not authorized to perform X"));

        underTest.checkInstanceTypes(STACK_ID);

        verify(gatewayConfigService, times(1)).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator, times(1)).getGrainOnAllHosts(any(), anyString());
        verify(cloudContextProvider, times(1)).getCloudContext(eq(stack));
        verify(credentialClientService, times(1)).getCloudCredential(any());
        verify(cloudPlatformConnectors, times(1)).get(any());
        verify(metadataCollector, times(1)).collectInstanceTypes(any(),
                argThat(list -> list.size() == 2 && list.containsAll(Set.of("instanceId1", "instanceId2"))));
        verifyNoInteractions(instanceMetaDataService, cloudbreakEventService);
    }

    private void prepeareProviderMocks(Stack stack, MetadataCollector metadataCollector) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        lenient().when(metadataCollector.collectInstanceTypes(eq(authenticatedContext), anyList()))
                .thenReturn(new InstanceTypeMetadata(Map.of("instanceId1", "large", "instanceId2", "large")));
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudContextProvider.getCloudContext(eq(stack))).thenReturn(CloudContext.Builder.builder().build());
    }

    private Stack stack(String templateInstanceType, String providerInstanceType1, String providerInstanceType2) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setType(StackType.WORKLOAD);

        InstanceMetaData instance1 = new InstanceMetaData();
        instance1.setDiscoveryFQDN("host1");
        instance1.setInstanceId("instanceId1");
        instance1.setProviderInstanceType(providerInstanceType1);

        InstanceMetaData instance2 = new InstanceMetaData();
        instance2.setDiscoveryFQDN("host2");
        instance2.setInstanceId("instanceId2");
        instance2.setProviderInstanceType(providerInstanceType2);

        InstanceGroup instanceGroup = new InstanceGroup();
        stack.setInstanceGroups(Set.of(instanceGroup));
        instanceGroup.setInstanceMetaData(Set.of(instance1, instance2));

        Template template = new Template();
        template.setInstanceType(templateInstanceType);
        instanceGroup.setTemplate(template);
        return stack;
    }

}