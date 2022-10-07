package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ScalingStatus.SUCCESS;
import static com.sequenceiq.periscope.api.model.ScalingStatus.TRIGGER_FAILED;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
class NodeDeletionServiceTest {

    private static final Long CLUSTER_ID = 1L;

    private static final String STACK_CRN = "test-stack-crn";

    private static final String HOSTGROUP = "compute";

    private static final String FQDN_PREIFX = "fqdn-";

    private static final String INSTANCE_ID_PREFIX = "test_instanceid_";

    private static final String NODE_DELETION_SUCCESS = "node deletion success";

    private static final String NODE_DELETION_FAILED = "node deletion failed";

    private static final Integer RUNNING_NODES = 10;

    private static final Integer STOPPED_NODES = 3;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private HistoryService historyService;

    @Mock
    private HttpNotificationSender notificationSender;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Captor
    private ArgumentCaptor<List<String>> stoppedHostIdsCaptor;

    @Captor
    private ArgumentCaptor<History> historyCaptor;

    @InjectMocks
    private NodeDeletionService underTest;

    @Test
    void testDeleteStoppedNodes() {
        Cluster cluster = getCluster();
        History history = getHistory(cluster, SUCCESS, NODE_DELETION_SUCCESS);
        StackV4Response stackResponse =
                MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(STACK_CRN, HOSTGROUP, FQDN_PREIFX, RUNNING_NODES, STOPPED_NODES);
        List<String> stoppedHostIds = IntStream.range(RUNNING_NODES, RUNNING_NODES + STOPPED_NODES)
                .mapToObj(i -> INSTANCE_ID_PREFIX + HOSTGROUP + i)
                .collect(Collectors.toList());

        doReturn(stackResponse).when(cloudbreakCommunicator).getByCrn(STACK_CRN);
        doReturn(NODE_DELETION_SUCCESS).when(messagesService).getMessage(anyString(), anyCollection());
        doReturn(history).when(historyService).createEntry(any(ScalingStatus.class), anyString(), any(Cluster.class));
        doCallRealMethod().when(stackResponseUtils).getStoppedCloudInstanceIdsInHostGroup(stackResponse, HOSTGROUP);

        underTest.deleteStoppedNodesIfPresent(cluster, HOSTGROUP);

        verify(cloudbreakCommunicator, times(1)).getByCrn(STACK_CRN);
        verify(cloudbreakCommunicator, times(1)).deleteInstancesForCluster(eq(cluster), stoppedHostIdsCaptor.capture());
        verify(notificationSender, times(1)).sendHistoryUpdateNotification(historyCaptor.capture(), eq(cluster));
        List<String> stoppedHostIdsResult = stoppedHostIdsCaptor.getValue();
        History historyResult = historyCaptor.getValue();

        assertThat(stoppedHostIdsResult).hasSize(STOPPED_NODES).hasSameElementsAs(stoppedHostIds);
        assertThat(historyResult.getScalingStatus()).isEqualTo(SUCCESS);
        assertThat(historyResult.getStatusReason()).isEqualTo(NODE_DELETION_SUCCESS);
    }

    @Test
    void testDeleteStoppedNodesFailed() {
        Cluster cluster = getCluster();
        History history = getHistory(cluster, TRIGGER_FAILED, NODE_DELETION_FAILED);
        StackV4Response stackResponse =
                MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(STACK_CRN, HOSTGROUP, FQDN_PREIFX, RUNNING_NODES, STOPPED_NODES);

        doReturn(stackResponse).when(cloudbreakCommunicator).getByCrn(STACK_CRN);
        doCallRealMethod().when(stackResponseUtils).getStoppedCloudInstanceIdsInHostGroup(stackResponse, HOSTGROUP);
        doThrow(new RuntimeException("test exception")).when(cloudbreakCommunicator).deleteInstancesForCluster(any(Cluster.class), anyList());
        doReturn(history).when(historyService).createEntry(any(ScalingStatus.class), anyString(), any(Cluster.class));
        doReturn(NODE_DELETION_FAILED).when(messagesService).getMessage(anyString(), anyCollection());

        underTest.deleteStoppedNodesIfPresent(cluster, HOSTGROUP);

        verify(cloudbreakCommunicator, times(1)).getByCrn(STACK_CRN);
        verify(cloudbreakCommunicator, times(1)).deleteInstancesForCluster(eq(cluster), stoppedHostIdsCaptor.capture());
        verify(notificationSender, times(1)).sendHistoryUpdateNotification(historyCaptor.capture(), eq(cluster));
        List<String> stoppedHostIdsResult = stoppedHostIdsCaptor.getValue();
        History historyResult = historyCaptor.getValue();

        assertThat(stoppedHostIdsResult).hasSize(STOPPED_NODES);
        assertThat(historyResult.getScalingStatus()).isEqualTo(TRIGGER_FAILED);
        assertThat(historyResult.getStatusReason()).isEqualTo(NODE_DELETION_FAILED);
    }

    @Test
    void testDeleteStoppedNodesForClusterWithNoStoppedNodes() {
        Cluster cluster = getCluster();
        StackV4Response stackResponse =
                MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(STACK_CRN, HOSTGROUP, FQDN_PREIFX, RUNNING_NODES, 0);

        doReturn(stackResponse).when(cloudbreakCommunicator).getByCrn(STACK_CRN);
        doCallRealMethod().when(stackResponseUtils).getStoppedCloudInstanceIdsInHostGroup(stackResponse, HOSTGROUP);

        underTest.deleteStoppedNodesIfPresent(cluster, HOSTGROUP);

        verify(cloudbreakCommunicator, times(1)).getByCrn(STACK_CRN);
        verify(cloudbreakCommunicator, never()).deleteInstancesForCluster(eq(cluster), anyList());
        verifyNoInteractions(notificationSender, historyService);
    }

    @Test
    void testDeleteStoppedNodesForClusterWithPolicyHostGroup() {
        Cluster cluster = getCluster();
        cluster.setLoadAlerts(emptySet());

        underTest.deleteStoppedNodesIfPresent(cluster, null);

        verify(cloudbreakCommunicator, never()).getByCrn(STACK_CRN);
        verify(cloudbreakCommunicator, never()).deleteInstancesForCluster(eq(cluster), anyList());
        verifyNoInteractions(notificationSender, historyService);
    }

    private Cluster getCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setStackCrn(STACK_CRN);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setCluster(cluster);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup(HOSTGROUP);
        loadAlert.setScalingPolicy(scalingPolicy);

        cluster.setLoadAlerts(Set.of(loadAlert));
        return cluster;
    }

    private History getHistory(Cluster cluster, ScalingStatus scalingStatus, String statusReason) {
        return new History(scalingStatus, statusReason, cluster.getStackCrn()).withCluster(cluster);
    }

}