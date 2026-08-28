package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RemoveLoadBalancerHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @InjectMocks
    private RemoveLoadBalancerHandler underTest;

    @Test
    void testDoAccept() throws Exception {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        CloudStack cloudStack = mock(CloudStack.class);

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setIp("10.1.1.1");
        loadBalancer.setSku(LoadBalancerSku.BASIC);
        loadBalancer.setType(LoadBalancerType.PRIVATE);
        LoadBalancerConfigDbWrapper providerConfig = new LoadBalancerConfigDbWrapper();
        AzureLoadBalancerConfigDb azureConfig = new AzureLoadBalancerConfigDb();
        azureConfig.setName("azureLbName");
        providerConfig.setAzureConfig(azureConfig);
        loadBalancer.setProviderConfig(providerConfig);

        LoadBalancer loadBalancerWithoutProviderConfig = new LoadBalancer();
        loadBalancerWithoutProviderConfig.setIp("10.1.1.1");
        loadBalancerWithoutProviderConfig.setSku(LoadBalancerSku.BASIC);
        loadBalancerWithoutProviderConfig.setType(LoadBalancerType.PRIVATE);

        Set<LoadBalancer> loadBalancers = Set.of(loadBalancer, loadBalancerWithoutProviderConfig);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancers);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(AzureConstants.PLATFORM);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        Resource dbResource = mock(Resource.class);
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getName()).thenReturn("azureLbName");
        when(resourceService.findAllByStackIdAndResourceTypeIn(STACK_ID, Set.of(ResourceType.AZURE_LOAD_BALANCER)))
                .thenReturn(List.of(dbResource));
        when(cloudResourceConverter.convert(dbResource)).thenReturn(cloudResource);

        RemoveLoadBalancerRequest request = new RemoveLoadBalancerRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<RemoveLoadBalancerRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertEquals(RemoveLoadBalancerResult.class, selectable.getClass());
        verify(skuMigrationService, times(1)).updateSkuToStandard(STACK_ID, loadBalancers);
        verify(resourceConnector, times(1)).deleteLoadBalancers(authenticatedContext, cloudStack, List.of(cloudResource));
    }

    @Test
    void testDoAcceptWithNoLbResources() throws Exception {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        CloudStack cloudStack = mock(CloudStack.class);

        Set<LoadBalancer> loadBalancers = Set.of();
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancers);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(AzureConstants.PLATFORM);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenReturn(List.of());

        RemoveLoadBalancerRequest request = new RemoveLoadBalancerRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<RemoveLoadBalancerRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertEquals(RemoveLoadBalancerResult.class, selectable.getClass());
        verify(resourceConnector, times(1)).deleteLoadBalancers(authenticatedContext, cloudStack, List.of());
        verify(skuMigrationService, times(1)).updateSkuToStandard(STACK_ID, loadBalancers);
    }

    @Test
    void testDoAcceptFailure() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenThrow(new RuntimeException("auth error"));

        RemoveLoadBalancerRequest request = new RemoveLoadBalancerRequest(mock(StackView.class), mock(CloudContext.class),
                mock(CloudCredential.class), cloudConnector, mock(CloudStack.class));
        HandlerEvent<RemoveLoadBalancerRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);
        assertNotNull(result);
        assertEquals(SkuMigrationFailedEvent.class, result.getClass());
        assertEquals(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), result.getSelector());
    }
}
