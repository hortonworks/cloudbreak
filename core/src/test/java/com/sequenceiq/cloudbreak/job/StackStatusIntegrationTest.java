package com.sequenceiq.cloudbreak.job;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.metrics.MetricsClient;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltSyncService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.UpdateHostsValidator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.ServiceStatusCheckerLogLocationDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StackStatusIntegrationTest.TestAppContext.class)
class StackStatusIntegrationTest {

    private static final Long STACK_ID = 123L;

    private static final String INSTANCE_1 = "i1";

    private static final String INSTANCE_2 = "i2";

    @Inject
    private StackStatusCheckerJob underTest;

    @MockBean
    private StackStatusCheckerConfig config;

    @MockBean
    private StackService stackService;

    @MockBean
    private StackDtoService stackDtoService;

    @MockBean
    private ClusterApiConnectors clusterApiConnectors;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private ClusterService clusterService;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private HostGroupService hostGroupService;

    @MockBean
    private ReactorFlowManager flowManager;

    @MockBean
    private MetricsClient metricsClient;

    @MockBean
    private SaltSyncService saltSyncService;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @MockBean
    private StackViewService stackViewService;

    @MockBean
    private Clock clock;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @MockBean
    private StackUtil stackUtil;

    @MockBean
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @MockBean
    private StackInstanceStatusChecker stackStatusChecker;

    @MockBean
    private RuntimeVersionService runtimeVersionService;

    @MockBean
    private EnvironmentService environmentClientService;

    @MockBean
    private ServiceStatusCheckerLogLocationDecorator serviceStatusCheckerLogLocationDecorator;

    @MockBean
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    private Stack stack;

    @Mock
    private StackDto stackDto;

    private List<InstanceMetadataView> runningInstances;

    private Map<HostName, Set<HealthCheck>> hostStatuses;

    @Mock
    private DetailedEnvironmentResponse environment;

    @MockBean
    private MeteringService meteringService;

    @BeforeEach
    void setUp() {
        setUpRunningInstances();
        setUpStack();
        setUpClusterApi();
        when(environmentClientService.getByCrnAsInternal(anyString())).thenReturn(environment);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap());
        when(serviceStatusCheckerLogLocationDecorator.decorate(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
    }

    private void setUpRunningInstances() {
        runningInstances = List.of(createInstance(INSTANCE_1), createInstance(INSTANCE_2));
    }

    private InstanceMetaData createInstance(String instanceName) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetaData.setInstanceId(instanceName);
        instanceMetaData.setDiscoveryFQDN(instanceName);
        return instanceMetaData;
    }

    private void setUpStack() {
        underTest.setLocalId(STACK_ID.toString());

        stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setVariant("AWS");
        cluster.setClusterManagerIp("192.168.0.1");
        cluster.setId(2L);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:stack");
        User creator = new User();
        creator.setUserId("user-id");
        stack.setCreator(creator);
        Workspace workspace = new Workspace();
        workspace.setId(564L);
        stack.setWorkspace(workspace);

        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(runningInstances);
    }

    private void setUpClusterApi() {
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);

        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        hostStatuses = new HashMap<>();

        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(new ExtendedHostStatuses(hostStatuses));
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN all instances are healthy " +
                    "THEN stack is still available"
    )
    void availableStackInstancesAreHealthy() throws JobExecutionException {
        setUpClusterStatus(ClusterStatus.STARTED);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackDto.getStatus()).thenReturn(AVAILABLE);
        setUpHealthForInstance(INSTANCE_1, HealthCheckResult.HEALTHY);
        setUpHealthForInstance(INSTANCE_2, HealthCheckResult.HEALTHY);
        setUpCloudVmInstanceStatuses(Map.of(
                INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED,
                INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED));
        underTest.executeJob(jobExecutionContext);

        verify(instanceMetaDataService, never()).findHostInStack(eq(STACK_ID), any());
        verify(hostGroupService, never()).getRepairViewByClusterIdAndName(anyLong(), anyString());
        verify(flowManager, never()).triggerClusterRepairFlow(anyLong(), any(), anyBoolean());
        verify(instanceMetaDataService, never()).saveAll(any());
        verify(clusterService, never()).updateClusterStatusByStackId(any(), any(), any());
        verify(clusterService).updateClusterCertExpirationState(stack.getCluster(), false, "");

        verify(instanceMetaDataService, never()).save(any());
        verify(stackUpdater, never()).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class));
        verify(stackUpdater, never()).updateStackStatus(eq(STACK_ID), any(), any());
        verify(meteringService, times(1)).scheduleSyncIfNotScheduled(eq(STACK_ID));
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN one instance goes down " +
                    "THEN stack is still available"
    )
    void availableStackOneInstanceGoesDown() throws JobExecutionException {
        setUpClusterStatus(ClusterStatus.STARTED);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackDto.getStatus()).thenReturn(AVAILABLE);
        setUpHealthForInstance(INSTANCE_1, HealthCheckResult.HEALTHY);
        setUpHealthForInstance(INSTANCE_2, HealthCheckResult.UNHEALTHY);
        setUpCloudVmInstanceStatuses(Map.of(
                INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED,
                INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER));
        underTest.executeJob(jobExecutionContext);

        verify(instanceMetaDataService, never()).findHostInStack(eq(STACK_ID), any());
        verify(hostGroupService, never()).getRepairViewByClusterIdAndName(anyLong(), anyString());
        verify(flowManager, never()).triggerClusterRepairFlow(anyLong(), any(), anyBoolean());
        verify(instanceMetaDataService, never()).saveAll(any());
        verify(clusterService, never()).updateClusterStatusByStackId(any(), any(), any());
        verify(clusterService).updateClusterCertExpirationState(stack.getCluster(), false, "");

        assertInstancesSavedWithStatuses(Map.of(INSTANCE_2, InstanceStatus.DELETED_BY_PROVIDER));

        verify(stackUpdater, never()).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class));
        verify(stackUpdater, never()).updateStackStatus(eq(STACK_ID), any(), any());
        verify(meteringService, times(1)).scheduleSyncIfNotScheduled(eq(STACK_ID));
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN all instances go down " +
                    "THEN stack is no more available"
    )
    void availableStackAllInstancesGoesDown() throws JobExecutionException {
        setUpClusterStatus(ClusterStatus.STARTED);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackDto.getStatus()).thenReturn(AVAILABLE);
        setUpHealthForInstance(INSTANCE_1, HealthCheckResult.HEALTHY);
        setUpHealthForInstance(INSTANCE_2, HealthCheckResult.UNHEALTHY);
        setUpCloudVmInstanceStatuses(Map.of(
                INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER,
                INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER));
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(runningInstances, List.of());
        underTest.executeJob(jobExecutionContext);

        verify(instanceMetaDataService, never()).findHostInStack(eq(STACK_ID), any());
        verify(hostGroupService, never()).getRepairViewByClusterIdAndName(anyLong(), anyString());
        verify(flowManager, never()).triggerClusterRepairFlow(anyLong(), any(), anyBoolean());
        verify(instanceMetaDataService, never()).saveAll(any());
        verify(clusterService, never()).updateClusterStatusByStackId(any(), any(), any());
        verify(clusterService).updateClusterCertExpirationState(stack.getCluster(), false, "");

        assertInstancesSavedWithStatuses(Map.of(
                INSTANCE_1, InstanceStatus.DELETED_BY_PROVIDER,
                INSTANCE_2, InstanceStatus.DELETED_BY_PROVIDER));

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE), any());
        verify(meteringService, never()).scheduleSyncIfNotScheduled(eq(STACK_ID));
    }

    @Test
    @DisplayName(
            "GIVEN an available stack with one instance down " +
                    "WHEN all instances go down " +
                    "THEN stack is no more available"
    )
    void availableStackWithOneInstanceDownAllInstancesGoesDown() throws JobExecutionException {
        setUpClusterStatus(ClusterStatus.STARTED);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        when(stackDto.getStatus()).thenReturn(AVAILABLE);
        setUpHealthForInstance(INSTANCE_1, HealthCheckResult.HEALTHY);
        setUpHealthForInstance(INSTANCE_2, HealthCheckResult.UNHEALTHY);
        setUpCloudVmInstanceStatuses(Map.of(
                INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER,
                INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER));
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(STACK_ID)).thenReturn(runningInstances, List.of());
        underTest.executeJob(jobExecutionContext);

        verify(instanceMetaDataService, never()).findHostInStack(eq(STACK_ID), any());
        verify(hostGroupService, never()).getRepairViewByClusterIdAndName(anyLong(), anyString());
        verify(flowManager, never()).triggerClusterRepairFlow(anyLong(), any(), anyBoolean());
        verify(instanceMetaDataService, never()).saveAll(any());
        verify(clusterService, never()).updateClusterStatusByStackId(any(), any(), any());
        verify(clusterService).updateClusterCertExpirationState(stack.getCluster(), false, "");

        assertInstancesSavedWithStatuses(Map.of(
                INSTANCE_1, InstanceStatus.DELETED_BY_PROVIDER,
                INSTANCE_2, InstanceStatus.DELETED_BY_PROVIDER));

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE), any());
        verify(meteringService, never()).scheduleSyncIfNotScheduled(eq(STACK_ID));
    }

    private void assertInstancesSavedWithStatuses(Map<String, InstanceStatus> instanceStatuses) {
        verify(instanceMetaDataService, times(instanceStatuses.keySet().size())).updateInstanceStatus(any(), any());
        for (Map.Entry<String, InstanceStatus> instanceStatusEntry : instanceStatuses.entrySet()) {
            verify(instanceMetaDataService, times(1))
                    .updateInstanceStatus(argThat(im -> instanceStatusEntry.getKey().equals(im.getInstanceId())),
                            argThat(status -> instanceStatusEntry.getValue().equals(status)));
        }
    }

    private void setUpCloudVmInstanceStatuses(Map<String, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus> instanceStatuses) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = instanceStatuses.entrySet().stream()
                .map(e -> createCloudVmInstanceStatus(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        when(stackStatusChecker.queryInstanceStatuses(eq(stackDto), any())).thenReturn(cloudVmInstanceStatuses);
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(String instanceId, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus) {
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null, "subnet-1", "az1"), instanceStatus);
    }

    private void setUpClusterStatus(ClusterStatus clusterStatus) {
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(new ClusterStatusResult(clusterStatus, ""));
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
    }

    private void setUpHealthForInstance(String fqdn, HealthCheckResult healthCheckResult) {
        hostStatuses.put(hostName(fqdn), Sets.newHashSet(
                new HealthCheck(HealthCheckType.HOST, healthCheckResult, Optional.empty(), Optional.empty())));
    }

    @Configuration
    @Import({
            StackStatusCheckerJob.class,
            StackSyncService.class,
            ClusterOperationService.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {

        @MockBean
        private StatusCheckerJobService jobService;

        @MockBean
        private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

        @MockBean
        private FlowLogService flowLogService;

        @MockBean
        private CloudbreakEventService eventService;

        @MockBean
        private ImageService imageService;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private ResourceService resourceService;

        @MockBean
        private ResourceAttributeUtil resourceAttributeUtil;

        @MockBean
        private FileSystemConfigService fileSystemConfigService;

        @MockBean
        private UsageLoggingUtil usageLoggingUtil;

        @MockBean
        private StatusToPollGroupConverter statusToPollGroupConverter;

        @MockBean
        private UpdateHostsValidator updateHostsValidator;

        @MockBean
        private CloudbreakMessagesService cloudbreakMessagesService;

        @MockBean
        private BlueprintService blueprintService;

        @MockBean
        private BlueprintValidatorFactory blueprintValidatorFactory;

        @MockBean
        private StackStopRestrictionService stackStopRestrictionService;

        @MockBean
        private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    }
}
