package com.sequenceiq.cloudbreak.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
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
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class StopCmServicesOnHostsInsteadOfDecommissionHandlerTest {

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DistroXUpgradeService distroXUpgradeService;

    @InjectMocks
    private StopCmServicesOnHostsInsteadOfDecommissionHandler underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @BeforeEach
    public void setUp() {
        lenient().when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getId()).thenReturn(1L);
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
    }

    @Test
    void testWhenStopRolesCompletes() throws CloudbreakException {
        Set<String> hostNamesToStop = Set.of("compute1", "compute2");
        StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        setClouderaManagerVersion("7.2.17");

        Selectable selectable = underTest.doAccept(newHandlerEvent(stopCmServicesOnHostsRequest));

        verify(clusterDecomissionService, times(1)).stopRolesOnHosts(hostNamesToStop, false);
        assertThat(((DecommissionResult) selectable).getHostNames()).containsOnly("compute1", "compute2");
    }

    @Test
    void testWhenStopRolesFails() throws CloudbreakException {
        Set<String> hostNamesToStop = Set.of("compute1", "compute2");
        StopCmServicesOnHostsRequest stopCmServicesOnHostsRequest = new StopCmServicesOnHostsRequest(
                1L, Set.of("compute"), hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        setClouderaManagerVersion("7.2.17");
        doThrow(new CloudbreakException("Stop failed")).when(clusterDecomissionService).stopRolesOnHosts(any(), anyBoolean());

        Selectable selectable = underTest.doAccept(newHandlerEvent(stopCmServicesOnHostsRequest));

        verify(clusterDecomissionService, times(1)).stopRolesOnHosts(hostNamesToStop, false);
        assertEquals("Stop roles failed on hosts: Stop failed", ((DecommissionResult) selectable).getStatusReason());
    }

    private HandlerEvent<StopCmServicesOnHostsRequest> newHandlerEvent(StopCmServicesOnHostsRequest request) {
        return new HandlerEvent<>(new Event<>(request));
    }

    private void setClouderaManagerVersion(String version) {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct();
        cdhProduct.setVersion(version);
    }

}