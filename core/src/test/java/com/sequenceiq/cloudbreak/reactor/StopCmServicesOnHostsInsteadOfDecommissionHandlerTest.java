package com.sequenceiq.cloudbreak.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.StopCmServicesOnHostsRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class StopCmServicesOnHostsInsteadOfDecommissionHandlerTest {

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private StopCmServicesOnHostsInsteadOfDecommissionHandler stopCmServicesOnHostsInsteadOfDecommissionHandler;

    @Test
    void doAcceptTest() throws CloudbreakException {
        StackDto stackDto = new StackDto();
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterDecomissionService clusterDecomissionService = mock(ClusterDecomissionService.class);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        Set<String> hostNamesToStop = Set.of("compute1.example.com", "compute2.example.com");
        StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        Event<StopCmServicesOnHostsRequest> stopCmServicesOnHostsRequestEvent = new Event<>(stopCmServicesOnHostsRequest);
        HandlerEvent<StopCmServicesOnHostsRequest> handlerEvent = new HandlerEvent<>(stopCmServicesOnHostsRequestEvent);
        Selectable selectable = stopCmServicesOnHostsInsteadOfDecommissionHandler.doAccept(handlerEvent);
        verify(clusterDecomissionService, times(1)).stopRolesOnHosts(hostNamesToStop);
        assertThat(((DecommissionResult) selectable).getHostNames()).containsOnly("compute1.example.com", "compute2.example.com");
    }

    @Test
    void doAcceptFailedTest() throws CloudbreakException {
        StackDto stackDto = new StackDto();
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterDecomissionService clusterDecomissionService = mock(ClusterDecomissionService.class);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        Set<String> hostNamesToStop = Set.of("compute1.example.com", "compute2.example.com");
        StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        Event<StopCmServicesOnHostsRequest> stopCmServicesOnHostsRequestEvent = new Event<>(stopCmServicesOnHostsRequest);
        HandlerEvent<StopCmServicesOnHostsRequest> handlerEvent = new HandlerEvent<>(stopCmServicesOnHostsRequestEvent);
        doThrow(new CloudbreakException("Stop failed")).when(clusterDecomissionService).stopRolesOnHosts(any());
        Selectable selectable = stopCmServicesOnHostsInsteadOfDecommissionHandler.doAccept(handlerEvent);
        verify(clusterDecomissionService, times(1)).stopRolesOnHosts(hostNamesToStop);
        assertEquals("Stop roles failed on hosts: Stop failed", ((DecommissionResult) selectable).getStatusReason());
    }
}