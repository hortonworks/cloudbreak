package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class StopStartDownscaleDecommissionViaCMHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final Long CLUSTER_ID = 101L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String INSTANCE_ID_PREFIX_RC = "i-a-";

    private static final String FQDN_PREFIX = "fqdn-";

    private static final String FQDN_PREFIX_RC = "fqdn-a-";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private CloudInstanceIdToInstanceMetaDataConverter instanceIdToInstanceMetaDataConverter;

    @InjectMocks
    private StopStartDownscaleDecommissionViaCMHandler underTest;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
        when(stackService.getByIdWithLists(any())).thenReturn(stack);
    }

    @Test
    void testAllDecommissioned() {
        testCollectDecommissionCombinationsInternal(5, 5, 5);
    }

    @Test
    void testCmHasMissingNodes() {
        testCollectDecommissionCombinationsInternal(5, 4, 4);
    }

    @Test
    void testCollectAndDecommissionReturnFewerNodes() {
        testCollectDecommissionCombinationsInternal(5, 4, 3);
    }

    @Test
    void testDecommissionReturnsFewerNodes() {
        testCollectDecommissionCombinationsInternal(5, 5, 3);
    }

    @Test
    void testNoNodesAvailableInCm() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 0;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verify(instanceMetaDataService, never()).updateInstanceStatuses(anyCollection(), any(InstanceStatus.class), anyString());

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testNoNodesFromCMDecommission() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 4;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream().map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verify(instanceMetaDataService).updateInstanceStatuses(eq(Collections.emptyList()), any(), anyString());

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testNoNodesInInitialDecommissionRequest() {
        int instancesToDecommissionCount = 0;
        int expcetedInstanceToCollectCount = 0;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(instanceMetaDataService, never()).updateInstanceStatuses(anyCollection(), any(InstanceStatus.class), anyString());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testErrorFromCmHostCollection() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        when(clusterDecomissionService.collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission)))
                .thenThrow(new RuntimeException("collectHostsToDecommissionError"));

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getErrorDetails().getMessage()).isEqualTo("collectHostsToDecommissionError");
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.selector()).isEqualTo("STOPSTARTDOWNSCALEDECOMMISSIONVIACMRESULT_ERROR");

        verify(instanceMetaDataService, never()).updateInstanceStatuses(anyCollection(), any(InstanceStatus.class), anyString());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testErrorFromCmCommission() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collected), anyLong()))
                .thenThrow(new RuntimeException("decommissionHostsError"));

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getErrorDetails().getMessage()).isEqualTo("decommissionHostsError");
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.selector()).isEqualTo("STOPSTARTDOWNSCALEDECOMMISSIONVIACMRESULT_ERROR");

        verify(instanceMetaDataService, never()).updateInstanceStatuses(anyCollection(), any(InstanceStatus.class), anyString());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testAdditionalInstancesWithServicesNotRunningToDecommission() {
        int instancesToDecommissionCount = 5;
        int expectedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        int recoveryCandidatesCount = 3;
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0,
                instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        List<InstanceMetadataView> recoveryCandidates = getInstancesToDecommission(100, recoveryCandidatesCount, INSTANCE_ID_PREFIX_RC, FQDN_PREFIX_RC);
        List<CloudInstance> recoveryCandidatesCloudInstances = generateCloudInstances(100, 3, INSTANCE_ID_PREFIX_RC);
        Map<String, InstanceMetadataView> collected = recoveryCandidates.stream().collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN,
                imv -> imv));
        collected.putAll(instancesToDecommission.stream().limit(expectedInstanceToCollectCount)
                .collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, i -> i)));

        List<InstanceMetadataView> combinedDecommissionList = Stream.of(instancesToDecommission, recoveryCandidates)
                .flatMap(Collection::stream).collect(Collectors.toList());

        List<InstanceMetadataView> decommissionedMetadataList = new ArrayList<>(collected.values());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = combinedDecommissionList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, recoveryCandidatesCloudInstances);

        doReturn(combinedDecommissionList).when(stack).getNotTerminatedInstanceMetaData();
        doCallRealMethod().when(stackService).getInstanceMetadata(any(), any());

        Set<String> rcHostIds = recoveryCandidatesCloudInstances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet());
        doReturn(recoveryCandidates).when(instanceIdToInstanceMetaDataConverter).getNotDeletedAndNotZombieInstances(anyList(), anyString(),
                eq(rcHostIds));

        doReturn(fqdnsDecommissioned).when(clusterDecomissionService).decommissionClusterNodesStopStart(anyMap(), anyLong());

        doReturn(collected).when(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), anySet());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);


        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);
        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;

        List<Long> decommissionedMetadataIdList = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getId)
                .sorted()
                .collect(Collectors.toList());

        ArgumentCaptor<List<Long>> argCap = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService).updateInstanceStatuses(argCap.capture(), eq(InstanceStatus.DECOMMISSIONED), anyString());
        assertThat(decommissionedMetadataIdList).hasSameElementsAs(argCap.getValue());

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount + recoveryCandidatesCount);
        assertThat(result.getNotDecommissionedHostFqdns()).isEmpty();
    }

    private void testCollectDecommissionCombinationsInternal(int instancesToDecommissionCount, int expcetedInstanceToCollectCount,
            int expectedInstancesDecommissionedCount) {
        List<InstanceMetadataView> instancesToDecommission = getInstancesToDecommission(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, Collections.emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        List<Long> decommissionedMetadataIdList = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getId)
                .sorted()
                .collect(Collectors.toList());
        ArgumentCaptor<List<Long>> argCap = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService).updateInstanceStatuses(argCap.capture(), eq(InstanceStatus.DECOMMISSIONED), anyString());
        assertThat(decommissionedMetadataIdList).hasSameElementsAs(argCap.getValue());

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collected), anyLong());

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verify(clusterDecomissionService).enterMaintenanceMode(eq(fqdnsDecommissioned));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    private void setupBasicMocks() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);

        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
    }

    private void setupAdditionalMocks(String hostGroupName, List<InstanceMetadataView> allInstanceMetadata,
            Map<String, InstanceMetadataView> collectedInstances, Set<String> decommissionedInstances) {
        lenient().when(stack.getNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadata);
        lenient().when(stackService.getInstanceMetadata(any(), any())).thenCallRealMethod();
        lenient().when(instanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(anyList(), anyString(), anySet()))
                .thenReturn(Collections.emptyList());

        Set<String> hostnames = allInstanceMetadata.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        lenient().when(clusterDecomissionService.collectHostsToRemove(eq(hostGroupName), eq(hostnames))).thenReturn(collectedInstances);

        lenient().when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collectedInstances), anyLong())).thenReturn(decommissionedInstances);
    }

    private List<InstanceMetadataView> getInstancesToDecommission(int startIndex, int count, String instanceIdPrefix, String fqdnPrefix) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (long i = startIndex; i < startIndex + count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(instanceIdPrefix + i);
            instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(fqdnPrefix + i);
            instanceMetaData.setPrivateId(i);
            instanceMetaData.setId(i);
            instances.add(instanceMetaData);
        }
        return instances;
    }

    private List<CloudInstance> generateCloudInstances(int startIndex, int count, String instanceIdPrefix) {
        List<CloudInstance> instances = new LinkedList<>();
        IntStream.range(startIndex, startIndex + count).forEach(i -> {
            CloudInstance instance = new CloudInstance(instanceIdPrefix + i, null, null, null,
                    null);
            instances.add(instance);
        });
        return instances;
    }
}
