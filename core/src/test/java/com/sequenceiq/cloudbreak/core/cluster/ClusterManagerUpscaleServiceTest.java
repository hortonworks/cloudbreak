package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.polling.ExtendedPollingResult.ExtendedPollingResultBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpscaleServiceTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final long INSTANCE_METADATA_ID = 3L;

    private static final String INSTANCE_FQDN = "host-3.example.com";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ClusterServiceRunner clusterServiceRunner;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @InjectMocks
    private ClusterManagerUpscaleService underTest;

    @Test
    public void testUpscaleIfTargetedUpscaleNotSupportedOrPrimaryGatewayChanged() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        NodeReachabilityResult nodeReachabilityResult = new NodeReachabilityResult(Set.of(mock(Node.class)), Set.of(mock(Node.class)));
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(nodeReachabilityResult);
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterServiceRunner.updateClusterManagerClientConfig(eq(stackDto))).thenReturn("clusterIp");
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("clusterIp"))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), true, false);

        verifyNoInteractions(targetedUpscaleSupportService);

        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.FALSE);
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 3), false, false);

        verifyNoMoreInteractions(clusterServiceRunner);
        verify(clusterApi, times(2)).waitForHosts(any());
        verify(clusterManagerDefaultConfigAdjuster, times(2)).adjustDefaultConfig(eq(stackDto), anyInt(), eq(false));
    }

    @Test
    public void testUpscaleIfPrimaryGatewayChangedAndGovCloud() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDto.isOnGovPlatformVariant()).thenReturn(true);
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        NodeReachabilityResult nodeReachabilityResult = new NodeReachabilityResult(Set.of(mock(Node.class)), Set.of(mock(Node.class)));
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(nodeReachabilityResult);
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterServiceRunner.updateClusterManagerClientConfig(eq(stackDto))).thenReturn("clusterIp");
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("clusterIp"))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), true, false);

        verifyNoInteractions(targetedUpscaleSupportService);

        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 3), false, false);

        verify(clusterHostServiceRunner, times(1)).removeSecurityConfigFromCMAgentsConfig(stackDto, nodeReachabilityResult.getReachableNodes());
        verifyNoMoreInteractions(clusterServiceRunner);
        verify(clusterApi, times(2)).waitForHosts(any());
        verify(clusterManagerDefaultConfigAdjuster, times(2)).adjustDefaultConfig(eq(stackDto), anyInt(), eq(false));
    }

    @Test
    public void testUpscaleIfTargetedUpscaleSupported() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(eq(stackDto.getStack()))).thenReturn(Boolean.TRUE);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 3), false, false);

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
    }

    @Test
    public void testUpscaleClusterManagerWhenTargetedUpscaleAndWaitForHostsTimedOutThenZombieNotificationSent() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(eq(stackDto.getStack()))).thenReturn(Boolean.TRUE);
        when(clusterApi.waitForHosts(any()))
                .thenReturn(new ExtendedPollingResultBuilder().withFailedInstanceIds(Set.of(INSTANCE_METADATA_ID)).timeout().build());
        String reason = "Upscaling in Cloudera Manager was not successful, waiting for hosts to be ready timed out.";
        when(messagesService.getMessage(ResourceEvent.CLUSTER_NODE_FAILURE_REASON_WAIT_FOR_HOSTS_TIMEOUT.getMessage())).thenReturn(reason);
        InstanceMetadataView failedInstance = mock(InstanceMetadataView.class);
        when(failedInstance.getId()).thenReturn(INSTANCE_METADATA_ID);
        when(failedInstance.getDiscoveryFQDN()).thenReturn(INSTANCE_FQDN);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(eq(STACK_ID))).thenReturn(List.of(failedInstance));

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), false, false);

        verify(instanceMetaDataService, times(1)).updateInstanceStatuses(eq(Set.of(INSTANCE_METADATA_ID)), eq(InstanceStatus.ZOMBIE), eq(reason));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.CLUSTER_NODES_MARKED_AS_ZOMBIE), eq(INSTANCE_FQDN), eq(reason));
    }

    @Test
    public void testUpscaleClusterManagerWhenRepairAndWaitForHostsTimedOut() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);
        when(clusterApi.waitForHosts(any()))
                .thenReturn(new ExtendedPollingResultBuilder().withFailedInstanceIds(Set.of(INSTANCE_METADATA_ID)).timeout().build());
        String reason = "Upscaling in Cloudera Manager was not successful, waiting for hosts to be ready timed out.";
        when(messagesService.getMessage(ResourceEvent.CLUSTER_NODE_FAILURE_REASON_WAIT_FOR_HOSTS_TIMEOUT.getMessage())).thenReturn(reason);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), false, true));

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
        verify(instanceMetaDataService, times(1)).updateInstanceStatuses(
                eq(Set.of(INSTANCE_METADATA_ID)), eq(InstanceStatus.ORCHESTRATION_FAILED), eq(reason));
        assertEquals("Upscaling cluster manager was not successful, waiting for hosts timed out for nodes: [3], " +
                "please check Cloudera Manager logs for further details.", exception.getMessage());
    }

    @Test
    public void testUpscaleClusterManagerWhenRepairAndWaitForHostsTimedOutWithoutInstanceIds() throws ClusterClientInitException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(eq(stackDto), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stackDto), eq("otherclusterIp"))).thenReturn(clusterApi);
        when(clusterApi.waitForHosts(any())).thenReturn(new ExtendedPollingResultBuilder().withFailedInstanceIds(Set.of()).timeout().build());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), false, true));

        assertEquals("Upscaling cluster manager was not successful, waiting for hosts timed out, " +
                "please check Cloudera Manager logs for further details.", exception.getMessage());

        when(clusterApi.waitForHosts(any())).thenReturn(new ExtendedPollingResultBuilder()
                .timeout().withException(new RuntimeException("customerror")).build());
        exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), false, true));

        assertTrue(exception.getMessage().contains("customerror"));

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi, times(2)).waitForHosts(any());
        verify(instanceMetaDataService, times(2)).updateInstanceStatuses(any(), eq(InstanceStatus.ORCHESTRATION_FAILED), any());
    }

    private StackDto getStackDto() {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;
    }

}
