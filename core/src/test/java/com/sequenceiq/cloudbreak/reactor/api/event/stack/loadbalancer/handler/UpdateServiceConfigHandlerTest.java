package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateServiceConfigHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String CLUSTER_NAME = "aCluster";

    private static final String HUE_SERVICE = "HUE";

    private static final String HUE_KNOX_PROXYHOSTS = "knox_proxyhosts";

    private static final String GATEWAY_FQDN = "gateway.example.com";

    private static final String CLUSTER_FQDN = "cluster.example.com";

    private static final String LOAD_BALANCER_FQDN = "lb.example.com";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @InjectMocks
    private UpdateServiceConfigHandler underTest;

    private StackDto stackDto;

    @BeforeEach
    void setUp() {
        stackDto = mock(StackDto.class);
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stack);
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.getName()).thenReturn(CLUSTER_NAME);
        when(cluster.getFqdn()).thenReturn(CLUSTER_FQDN);
        when(stackDto.getCluster()).thenReturn(cluster);
        InstanceMetadataView primaryGateway = mock(InstanceMetadataView.class);
        when(primaryGateway.getDiscoveryFQDN()).thenReturn(GATEWAY_FQDN);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(primaryGateway);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(STACK_ID)).thenReturn(LOAD_BALANCER_FQDN);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
    }

    @Test
    void testDoAcceptWhenHueIsPresentThenKnoxProxyhostsIsUpdatedWithEveryProxyHost() throws Exception {
        when(clusterApi.isServicePresentOrFail(CLUSTER_NAME, HUE_SERVICE)).thenReturn(true);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);

        Selectable result = underTest.doAccept(handlerEvent());

        assertEquals(UpdateServiceConfigSuccess.class, result.getClass());
        ArgumentCaptor<String> proxyhostsCaptor = ArgumentCaptor.forClass(String.class);
        verify(clusterModificationService).updateServiceConfigAndRestartService(eq(HUE_SERVICE), eq(HUE_KNOX_PROXYHOSTS), proxyhostsCaptor.capture());
        assertEquals(Set.of(GATEWAY_FQDN, CLUSTER_FQDN, LOAD_BALANCER_FQDN), Set.of(proxyhostsCaptor.getValue().split(",")));
        verify(clusterHostServiceRunner).updateClusterConfigs(stackDto);
    }

    @Test
    void testDoAcceptWhenHueIsNotPresentThenKnoxProxyhostsUpdateIsSkippedButClusterConfigsAreStillUpdated() throws Exception {
        when(clusterApi.isServicePresentOrFail(CLUSTER_NAME, HUE_SERVICE)).thenReturn(false);

        Selectable result = underTest.doAccept(handlerEvent());

        assertEquals(UpdateServiceConfigSuccess.class, result.getClass());
        verify(clusterApi, never()).clusterModificationService();
        verify(clusterHostServiceRunner).updateClusterConfigs(stackDto);
    }

    @Test
    void testDoAcceptWhenHuePresenceCannotBeDeterminedThenTheFlowFailsInsteadOfSilentlySkippingTheUpdate() throws Exception {
        when(clusterApi.isServicePresentOrFail(CLUSTER_NAME, HUE_SERVICE)).thenThrow(new CloudbreakException("Cloudera Manager is not reachable"));

        Selectable result = underTest.doAccept(handlerEvent());

        assertEquals(UpdateServiceConfigFailure.class, result.getClass());
        verify(clusterHostServiceRunner, never()).updateClusterConfigs(any());
    }

    private HandlerEvent<UpdateServiceConfigRequest> handlerEvent() {
        return new HandlerEvent<>(new Event<>(new UpdateServiceConfigRequest(STACK_ID)));
    }
}
