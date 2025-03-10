package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.LOAD_BALANCER_SKU_IS_STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancerConfigDbWrapper;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure.AzureLoadBalancerConfigDb;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class CheckSkuHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @InjectMocks
    private CheckSkuHandler underTest;

    @Test
    public void testDoAcceptForceFalse() {
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
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(loadBalancer));
        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        ArgumentCaptor<List> lbListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        when(resourceConnector.describeLoadBalancers(eq(authenticatedContext), eq(cloudStack), lbListArgumentCaptor.capture()))
                .thenReturn(List.of(new CloudLoadBalancer(LoadBalancerType.PRIVATE, LoadBalancerSku.BASIC, false)));

        CheckSkuRequest request = new CheckSkuRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack, false);
        HandlerEvent<CheckSkuRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        verify(skuMigrationService, times(0)).updateSkuToStandard(Set.of(loadBalancer));
        verifyNoInteractions(flowMessageService);
        List<CloudLoadBalancerMetadata> loadBalancerMetadataListToDescribe = lbListArgumentCaptor.getValue();
        assertEquals(1, loadBalancerMetadataListToDescribe.size());
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = loadBalancerMetadataListToDescribe.get(0);
        assertEquals("azureLbName", cloudLoadBalancerMetadata.getName());
        assertEquals("10.1.1.1", cloudLoadBalancerMetadata.getIp());
        assertEquals(LoadBalancerType.PRIVATE, cloudLoadBalancerMetadata.getType());

        assertEquals(CheckSkuResult.class, selectable.getClass());
    }

    @Test
    public void testDoAcceptForceFalseButStandardLBFoundOnProviderSide() {
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
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of(loadBalancer));
        CloudConnector cloudConnector = mock(CloudConnector.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        ArgumentCaptor<List> lbListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        when(resourceConnector.describeLoadBalancers(eq(authenticatedContext), eq(cloudStack), lbListArgumentCaptor.capture()))
                .thenReturn(List.of(new CloudLoadBalancer(LoadBalancerType.PRIVATE, LoadBalancerSku.STANDARD, false)));

        CheckSkuRequest request = new CheckSkuRequest(stack, cloudContext, cloudCredential, cloudConnector, cloudStack, false);
        HandlerEvent<CheckSkuRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        verify(skuMigrationService, times(1)).updateSkuToStandard(Set.of(loadBalancer));
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), LOAD_BALANCER_SKU_IS_STANDARD);
        List<CloudLoadBalancerMetadata> loadBalancerMetadataListToDescribe = lbListArgumentCaptor.getValue();
        assertEquals(1, loadBalancerMetadataListToDescribe.size());
        CloudLoadBalancerMetadata cloudLoadBalancerMetadata = loadBalancerMetadataListToDescribe.get(0);
        assertEquals("azureLbName", cloudLoadBalancerMetadata.getName());
        assertEquals("10.1.1.1", cloudLoadBalancerMetadata.getIp());
        assertEquals(LoadBalancerType.PRIVATE, cloudLoadBalancerMetadata.getType());

        assertEquals(SkuMigrationFinished.class, selectable.getClass());
    }

    @Test
    public void testDoAcceptButForceTrue() {
        CheckSkuRequest request = new CheckSkuRequest(mock(StackView.class), mock(CloudContext.class), mock(CloudCredential.class), mock(CloudConnector.class),
                mock(CloudStack.class), true);
        HandlerEvent<CheckSkuRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        verifyNoInteractions(loadBalancerPersistenceService);
        verifyNoInteractions(skuMigrationService);
        verifyNoInteractions(flowMessageService);

        assertEquals(CheckSkuResult.class, selectable.getClass());
    }
}