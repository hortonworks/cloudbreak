package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_EXCLUDE_LOST_NODES;
import static java.util.Collections.emptyList;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
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
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
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
import com.sequenceiq.cloudbreak.service.autoscale.PeriscopeClientService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class StopStartDownscaleDecommissionViaCMHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Long STACK_ID = 100L;

    private static final String RESOURCE_CRN = "resource_crn";

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
    private PeriscopeClientService periscopeClientService;

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

    @Mock
    private ClusterHealthService clusterHealthService;

    @BeforeEach
    void setUp() {
        setupBasicMocks();
        when(stackService.getByIdWithLists(any())).thenReturn(stack);
    }

    @Test
    void testAllDecommissioned() throws Exception {
        testCollectDecommissionCombinationsInternal(5, 5, 5);
    }

    @Test
    void testCmHasMissingNodes() throws Exception {
        testCollectDecommissionCombinationsInternal(5, 4, 4);
    }

    @Test
    void testCollectAndDecommissionReturnFewerNodes() throws Exception {
        testCollectDecommissionCombinationsInternal(5, 4, 3);
    }

    @Test
    void testDecommissionReturnsFewerNodes() throws Exception {
        testCollectDecommissionCombinationsInternal(5, 5, 3);
    }

    @Test
    void testAllDecommissionedWithYarnRecommendation() throws Exception {
        testCollectDecommissionCombinationsInternalWithYarnRecommendation(5, 5, 4);
    }

    @Test
    void testNoNodesAvailableInCm() {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 0;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
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
    void testNoNodesFromCMDecommission() throws Exception {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 4;
        int expectedInstancesDecommissionedCount = 0;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());

        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenThrow(new TimeoutException());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(instancesToDecommissionCount - expectedInstancesDecommissionedCount);

        verify(instanceMetaDataService).updateInstanceStatuses(eq(emptyList()), any(), anyString());

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
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
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
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
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
    void testErrorFromCmCommission() throws Exception {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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

        List<String> instanceIds = collected.values().stream().map(InstanceMetadataView::getInstanceId).toList();
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenReturn(instanceIds);

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collected), anyLong()))
                .thenThrow(new RuntimeException("decommissionHostsError"));

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
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
    void testErrorAsNoInstancesToDecommission() throws Exception {
        int instancesToDecommissionCount = 5;
        int expcetedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
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

        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenReturn(List.of());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);
        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getNotDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getDecommissionedHostFqdns()).hasSize(0);
        assertThat(result.getErrorDetails().getMessage())
                .isEqualTo("This Node(s) 'fqdn-4, fqdn-1, fqdn-0, fqdn-3, fqdn-2' have jobs running on them, cannot decommission them");
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.selector()).isEqualTo("STOPSTARTDOWNSCALEDECOMMISSIONVIACMRESULT_ERROR");

        verify(instanceMetaDataService, never()).updateInstanceStatuses(anyCollection(), any(InstanceStatus.class), anyString());
        verifyNoMoreInteractions(flowMessageService);
        verifyNoMoreInteractions(clusterDecomissionService);
    }

    @Test
    void testAdditionalInstancesWithServicesNotRunningToDecommission() throws Exception {
        int instancesToDecommissionCount = 5;
        int expectedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        int recoveryCandidatesCount = 3;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> recoveryCandidates = getInstancesWithStatus(100, recoveryCandidatesCount, INSTANCE_ID_PREFIX_RC, FQDN_PREFIX_RC,
                InstanceStatus.SERVICES_HEALTHY);
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
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenThrow(new TimeoutException());
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

    @Test
    void testInstancesWithServiceNotRunningDecommissionYarnReverification() throws Exception {
        int instancesToDecommissionCount = 5;
        int expectedInstanceToCollectCount = 5;
        int expectedInstancesDecommissionedCount = 5;
        int recoveryCandidatesCount = 3;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> recoveryCandidates = getInstancesWithStatus(100, recoveryCandidatesCount, INSTANCE_ID_PREFIX_RC, FQDN_PREFIX_RC,
                InstanceStatus.SERVICES_HEALTHY);
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
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenReturn(instancesToDecommission.stream()
                .map(InstanceMetadataView::getInstanceId).collect(Collectors.toList()));
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

    @Test
    void testExcludeLostNodesFromCmDecommission() throws Exception {
        int instancesToDecommissionCount = 5;
        int lostInstancesToExcludeCount = 2;
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> lostInstances = getInstancesWithStatus(instancesToDecommissionCount, lostInstancesToExcludeCount, INSTANCE_ID_PREFIX,
                FQDN_PREFIX, InstanceStatus.SERVICES_UNHEALTHY);
        List<InstanceMetadataView> combined = Stream.of(instancesToDecommission, lostInstances).flatMap(Collection::stream).toList();

        Map<String, InstanceMetadataView> collected = combined.stream().collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, Function.identity()));
        Map<String, InstanceMetadataView> collectedInstanceDecommissioned = instancesToDecommission.stream()
                .collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, imv -> imv));
        Set<String> fqdnsDecommissioned = instancesToDecommission.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());
        Set<String> lostHostNames = lostInstances.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, combined, collected, fqdnsDecommissioned);
        when(clusterHealthService.getDisconnectedNodeManagers()).thenReturn(lostHostNames);
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenThrow(new TimeoutException());

        Set<Long> instanceIdsToDecommission = combined.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        Set<String> hostnamesToDecommission = combined.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());

        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(instancesToDecommission.size());
        assertThat(result.getNotDecommissionedHostFqdns()).isEmpty();

        List<Long> decomissionedInstanceIds = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).sorted().toList();

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService).updateInstanceStatuses(captor.capture(), eq(InstanceStatus.DECOMMISSIONED), anyString());
        assertThat(captor.getValue()).hasSameElementsAs(decomissionedInstanceIds);

        verify(clusterDecomissionService).collectHostsToRemove(eq(INSTANCE_GROUP_NAME), eq(hostnamesToDecommission));
        verify(clusterDecomissionService).decommissionClusterNodesStopStart(eq(collectedInstanceDecommissioned), anyLong());

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verify(clusterDecomissionService).enterMaintenanceMode(eq(fqdnsDecommissioned));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE), eq(String.valueOf(fqdnsDecommissioned.size())));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(CLUSTER_SCALING_STOPSTART_DOWNSCALE_EXCLUDE_LOST_NODES),
                eq(String.valueOf(lostInstances.size())), eq(String.join(", ",
                        lostInstances.stream().map(InstanceMetadataView::getDiscoveryFQDN).toList())));
    }

    private void testCollectDecommissionCombinationsInternal(int instancesToDecommissionCount, int expcetedInstanceToCollectCount,
            int expectedInstancesDecommissionedCount) throws Exception {
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().limit(expectedInstancesDecommissionedCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = decommissionedMetadataList.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenThrow(new TimeoutException());

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
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

    private void testCollectDecommissionCombinationsInternalWithYarnRecommendation(int instancesToDecommissionCount, int expcetedInstanceToCollectCount,
            int expectedInstancesDecommissionedCount) throws Exception {
        List<InstanceMetadataView> instancesToDecommission = getInstancesWithStatus(0, instancesToDecommissionCount, INSTANCE_ID_PREFIX, FQDN_PREFIX,
                InstanceStatus.SERVICES_HEALTHY);
        Map<String, InstanceMetadataView> collected =
                instancesToDecommission.stream().limit(expcetedInstanceToCollectCount).collect(Collectors.toMap(i -> i.getDiscoveryFQDN(), i -> i));
        List<InstanceMetadataView> decommissionedMetadataList =
                collected.values().stream().filter(i -> !i.getDiscoveryFQDN().equals("fqdn-0")).collect(Collectors.toList());
        List<InstanceMetadataView> initialDecommissionedMetadataList =
                collected.values().stream().limit(instancesToDecommissionCount).collect(Collectors.toList());
        Set<String> fqdnsDecommissioned = initialDecommissionedMetadataList.stream()
                .filter(i -> !i.getDiscoveryFQDN().equals("fqdn-0"))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        List<String> instanceIds = instancesToDecommission.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        instanceIds.remove(0);
        setupAdditionalMocks(INSTANCE_GROUP_NAME, instancesToDecommission, collected, fqdnsDecommissioned);
        when(periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN)).thenReturn(instanceIds);

        Set<Long> instanceIdsToDecommission = instancesToDecommission.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toUnmodifiableSet());
        Set<String> hostnamesToDecommission = instancesToDecommission.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(Collectors.toUnmodifiableSet());

        StopStartDownscaleDecommissionViaCMRequest request =
                new StopStartDownscaleDecommissionViaCMRequest(1L, INSTANCE_GROUP_NAME, instanceIdsToDecommission, emptyList());
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StopStartDownscaleDecommissionViaCMResult.class);

        StopStartDownscaleDecommissionViaCMResult result = (StopStartDownscaleDecommissionViaCMResult) selectable;
        assertThat(result.getDecommissionedHostFqdns()).hasSize(expectedInstancesDecommissionedCount);

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
        lenient().when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);

        lenient().when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        lenient().when(clusterApi.clusterHealthService()).thenReturn(clusterHealthService);
    }

    private void setupAdditionalMocks(String hostGroupName, List<InstanceMetadataView> allInstanceMetadata,
            Map<String, InstanceMetadataView> collectedInstances, Set<String> decommissionedInstances) {
        lenient().when(stack.getNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadata);
        lenient().when(stackService.getInstanceMetadata(any(), any())).thenCallRealMethod();
        lenient().when(instanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(anyList(), anyString(), anySet()))
                .thenReturn(emptyList());

        Set<String> hostnames = allInstanceMetadata.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toUnmodifiableSet());

        lenient().when(clusterDecomissionService.collectHostsToRemove(eq(hostGroupName), eq(hostnames))).thenReturn(collectedInstances);

        lenient().when(clusterDecomissionService.decommissionClusterNodesStopStart(eq(collectedInstances), anyLong())).thenReturn(decommissionedInstances);
    }

    private List<InstanceMetadataView> getInstancesWithStatus(int startIndex, int count, String instanceIdPrefix, String fqdnPrefix, InstanceStatus status) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        for (long i = startIndex; i < startIndex + count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(instanceIdPrefix + i);
            instanceMetaData.setInstanceStatus(status);
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
