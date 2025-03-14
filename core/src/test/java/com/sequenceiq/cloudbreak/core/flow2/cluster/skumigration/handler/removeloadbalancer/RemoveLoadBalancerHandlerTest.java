package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RemoveLoadBalancerHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @InjectMocks
    private RemoveLoadBalancerHandler underTest;

    @Test
    public void testDoAccept() {
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
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        RemoveLoadBalancerRequest request = new RemoveLoadBalancerRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        HandlerEvent<RemoveLoadBalancerRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertEquals(RemoveLoadBalancerResult.class, selectable.getClass());
        verify(skuMigrationService, times(1)).updateSkuToStandard(loadBalancers);
        verify(resourceConnector, times(1)).deleteLoadBalancers(authenticatedContext, cloudStack, List.of("azureLbName"));
    }

    @Test
    void testDoAcceptFailure() {
        when(loadBalancerPersistenceService.findByStackId(any())).thenThrow(new RuntimeException("error"));

        RemoveLoadBalancerRequest request = new RemoveLoadBalancerRequest(mock(StackView.class), mock(CloudContext.class),
                mock(CloudCredential.class), mock(CloudConnector.class), mock(CloudStack.class));
        HandlerEvent<RemoveLoadBalancerRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);
        assertNotNull(result);
        assertEquals(SkuMigrationFailedEvent.class, result.getClass());
        assertEquals(SkuMigrationFlowEvent.SKU_MIGRATION_FAILED_EVENT.event(), result.getSelector());
    }
}