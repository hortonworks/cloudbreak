package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DECOMMISSION_FAILED_FORCE_DELETE_CONTINUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class DecommissionHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 2L;

    private static final String HOST_GROUP_NAME = "hostGroup";

    private static final String FQDN = "hos1";

    private static final Long PRIVATE_ID = 3L;

    @InjectMocks
    private DecommissionHandler underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private FreeIpaCleanupService freeIpaCleanupService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    private Stack stack;

    private Cluster cluster;

    private InstanceMetaData instance;

    @Captor
    private ArgumentCaptor<Event<DecommissionResult>> captor;

    @BeforeEach
    public void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        instance = new InstanceMetaData();
        instance.setDiscoveryFQDN(FQDN);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(instance));
        stack.setInstanceGroups(Set.of(instanceGroup));
        stack.setResourceCrn(CrnTestUtil.getDatahubCrnBuilder().setAccountId("a1").setResource("r1").build().toString());

        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
    }

    @Test
    public void shouldContinueSingleHostDecommissionIfForcedAndFailed() throws CloudbreakException, ClusterClientInitException {
        when(entitlementService.bulkHostsRemovalFromCMSupported(any())).thenReturn(Boolean.FALSE);
        when(stackDto.getInstanceMetadata(eq(PRIVATE_ID))).thenReturn(Optional.of(instance));
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterDecomissionService.collectHostsToRemove(HOST_GROUP_NAME, Set.of(FQDN))).thenReturn(Map.of(FQDN, instance));
        doThrow(new CloudbreakException("Restart stale services failed.")).when(clusterDecomissionService).restartStaleServices(true);

        underTest.accept(new Event<>(new Event.Headers(), forcedDecommissionRequest()));

        verify(eventService).fireCloudbreakEvent(STACK_ID, "UPDATE_IN_PROGRESS", CLUSTER_DECOMMISSION_FAILED_FORCE_DELETE_CONTINUE,
                List.of("Restart stale services failed."));
        verify(eventBus).notify(eq("DECOMMISSIONRESULT"), captor.capture());
        Event<DecommissionResult> resultEvent = captor.getValue();
        assertNotNull(resultEvent);
        DecommissionResult decommissionResult = resultEvent.getData();
        assertEquals("", decommissionResult.getErrorPhase());
        assertNull(decommissionResult.getErrorDetails());
        assertEquals(Set.of(FQDN), decommissionResult.getHostNames());
        verify(clusterDecomissionService, never()).removeHostsFromCluster(anyList());
        verify(clusterDecomissionService, never()).decommissionClusterNodes(anyMap());
        verify(clusterDecomissionService, times(1)).deleteHostFromCluster(any());
    }

    @Test
    public void shouldContinueSingleHostDecommissionFailIfNotForced() throws CloudbreakException, ClusterClientInitException {
        when(entitlementService.bulkHostsRemovalFromCMSupported(any())).thenReturn(Boolean.FALSE);
        when(stackDto.getInstanceMetadata(eq(PRIVATE_ID))).thenReturn(Optional.of(instance));
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterDecomissionService.collectHostsToRemove(HOST_GROUP_NAME, Set.of(FQDN))).thenReturn(Map.of(FQDN, instance));
        when(clusterDecomissionService.decommissionClusterNodes(Map.of(FQDN, instance))).thenReturn(Set.of(FQDN));
        CloudbreakException cloudbreakException = new CloudbreakException("Restart stale services failed.");
        doThrow(cloudbreakException).when(clusterDecomissionService).restartStaleServices(false);

        underTest.accept(new Event<>(new Event.Headers(), decommissionRequest()));

        verify(eventBus).notify(eq("DECOMMISSIONRESULT_ERROR"), captor.capture());
        Event<DecommissionResult> resultEvent = captor.getValue();
        assertNotNull(resultEvent);
        DecommissionResult decommissionResult = resultEvent.getData();
        assertEquals("Restart stale services failed.", decommissionResult.getStatusReason());
        assertEquals("", decommissionResult.getErrorPhase());
        assertEquals(cloudbreakException, decommissionResult.getErrorDetails());
        assertEquals(Set.of(FQDN), decommissionResult.getHostNames());
        verify(clusterDecomissionService, never()).removeHostsFromCluster(anyList());
        verify(clusterDecomissionService, times(1)).decommissionClusterNodes(anyMap());
        verify(clusterDecomissionService, times(1)).deleteHostFromCluster(any());
    }

    @Test
    public void testBulkHostDecommission() throws ClusterClientInitException {
        when(entitlementService.bulkHostsRemovalFromCMSupported(any())).thenReturn(Boolean.TRUE);
        when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of(CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_14.getVersion()));
        when(stackDto.getInstanceMetadata(eq(PRIVATE_ID))).thenReturn(Optional.of(instance));
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterDecomissionService.collectHostsToRemove(HOST_GROUP_NAME, Set.of(FQDN))).thenReturn(Map.of(FQDN, instance));

        underTest.accept(new Event<>(new Event.Headers(), decommissionRequest()));

        verify(eventBus).notify(eq("DECOMMISSIONRESULT"), captor.capture());
        Event<DecommissionResult> resultEvent = captor.getValue();
        assertNotNull(resultEvent);
        DecommissionResult decommissionResult = resultEvent.getData();
        assertNull(decommissionResult.getStatusReason());
        assertNotNull(decommissionResult.getErrorPhase());
        assertEquals(Set.of(FQDN), decommissionResult.getHostNames());
        verify(clusterDecomissionService, times(2)).removeHostsFromCluster(List.of(instance));
        verify(clusterDecomissionService, never()).decommissionClusterNodes(anyMap());
        verify(clusterDecomissionService, never()).deleteHostFromCluster(any());
    }

    private DecommissionRequest forcedDecommissionRequest() {
        return decommissionRequest(true);
    }

    private DecommissionRequest decommissionRequest() {
        return decommissionRequest(false);
    }

    private DecommissionRequest decommissionRequest(boolean forced) {
        return new DecommissionRequest(STACK_ID, Set.of(HOST_GROUP_NAME), Set.of(PRIVATE_ID), new ClusterDownscaleDetails(forced, false, false));
    }

}