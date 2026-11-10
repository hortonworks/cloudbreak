package com.sequenceiq.cloudbreak.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.StopCmServicesOnHostsRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

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
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata("compute1", InstanceStatus.SERVICES_HEALTHY),
                createInstanceMetadata("compute2", InstanceStatus.SERVICES_HEALTHY)));

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

    @Test
    void testWhenStopRolesFailsLogsOnlyHostsSentToCm() throws CloudbreakException {
        StopCmServicesOnHostsRequest request = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                Set.of("compute1", "compute2"), Set.of(1L, 2L), new ClusterDownscaleDetails());
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata("compute1", InstanceStatus.DELETED_ON_PROVIDER_SIDE),
                createInstanceMetadata("compute2", InstanceStatus.SERVICES_HEALTHY)));
        CloudbreakException cause = new CloudbreakException("Stop failed");
        doThrow(cause).when(clusterDecomissionService).stopRolesOnHosts(Set.of("compute2"), false);
        Logger logger = (Logger) LoggerFactory.getLogger(StopCmServicesOnHostsInsteadOfDecommissionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            DecommissionResult result = (DecommissionResult) underTest.doAccept(newHandlerEvent(request));

            assertThat(result.getStatusReason()).isEqualTo("Stop roles failed on hosts: Stop failed");
            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .filteredOn(message -> message.startsWith("Stop roles failed on hosts:"))
                    .containsExactly("Stop roles failed on hosts: [compute2]");
            verify(clusterDecomissionService).stopRolesOnHosts(Set.of("compute2"), false);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @EnumSource(value = InstanceStatus.class, names = {"DELETED_ON_PROVIDER_SIDE", "DELETED_BY_PROVIDER"})
    void testWhenOneHostIsDeletedOnProvider(InstanceStatus deletedStatus) throws CloudbreakException {
        Set<String> hostNamesToStop = Set.of("compute1", "compute2");
        StopCmServicesOnHostsRequest request = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata("compute1", deletedStatus), createInstanceMetadata("compute2", InstanceStatus.SERVICES_HEALTHY)));

        DecommissionResult result = (DecommissionResult) underTest.doAccept(newHandlerEvent(request));

        verify(clusterDecomissionService).stopRolesOnHosts(Set.of("compute2"), false);
        verify(distroXUpgradeService, never()).isGracefulStopServicesNeeded(any());
        assertThat(result.getHostNames()).containsExactlyInAnyOrderElementsOf(hostNamesToStop);
    }

    @ParameterizedTest
    @EnumSource(value = InstanceStatus.class, names = {"DELETED_ON_PROVIDER_SIDE", "DELETED_BY_PROVIDER"})
    void testWhenAllHostsAreDeletedOnProvider(InstanceStatus deletedStatus) throws CloudbreakException {
        Set<String> hostNamesToStop = Set.of("compute1", "compute2");
        StopCmServicesOnHostsRequest request = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                hostNamesToStop, Set.of(1L, 2L), new ClusterDownscaleDetails());
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata("compute1", deletedStatus), createInstanceMetadata("compute2", deletedStatus)));

        DecommissionResult result = (DecommissionResult) underTest.doAccept(newHandlerEvent(request));

        verify(clusterDecomissionService, never()).stopRolesOnHosts(any(), anyBoolean());
        assertThat(result.getHostNames()).containsExactlyInAnyOrderElementsOf(hostNamesToStop);
    }

    @Test
    void testWhenSingleLiveHostNeedsGracefulStop() throws CloudbreakException {
        StopCmServicesOnHostsRequest request = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                Set.of("compute1"), Set.of(1L), new ClusterDownscaleDetails());
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata("compute1", InstanceStatus.SERVICES_HEALTHY)));
        when(distroXUpgradeService.isGracefulStopServicesNeeded(stackDto)).thenReturn(true);

        DecommissionResult result = (DecommissionResult) underTest.doAccept(newHandlerEvent(request));

        verify(clusterDecomissionService).stopRolesOnHosts(Set.of("compute1"), true);
        assertThat(result.getHostNames()).containsOnly("compute1");
    }

    @Test
    void testWhenDeletedInstanceHasNoDiscoveryFqdn() throws CloudbreakException {
        StopCmServicesOnHostsRequest request = new StopCmServicesOnHostsRequest(1L, Set.of("compute"),
                Set.of("compute1", "compute2"), Set.of(1L, 2L), new ClusterDownscaleDetails());
        when(stackDto.getAllAvailableAndProviderDeletedInstances()).thenReturn(List.of(
                createInstanceMetadata(null, InstanceStatus.DELETED_ON_PROVIDER_SIDE)));

        DecommissionResult result = (DecommissionResult) underTest.doAccept(newHandlerEvent(request));

        verify(clusterDecomissionService).stopRolesOnHosts(request.getHostNamesToStop(), false);
        assertThat(result.getHostNames()).containsOnly("compute1", "compute2");
    }

    private InstanceMetadataView createInstanceMetadata(String hostname, InstanceStatus status) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostname);
        instanceMetaData.setInstanceStatus(status);
        return instanceMetaData;
    }

    private HandlerEvent<StopCmServicesOnHostsRequest> newHandlerEvent(StopCmServicesOnHostsRequest request) {
        return new HandlerEvent<>(new Event<>(request));
    }

    private void setClouderaManagerVersion(String version) {
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct();
        cdhProduct.setVersion(version);
    }

}