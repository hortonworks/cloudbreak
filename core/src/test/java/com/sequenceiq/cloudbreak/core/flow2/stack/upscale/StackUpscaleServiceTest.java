package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.TestUtil.instanceMetaData;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REPAIR_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPSCALE_QUOTA_ISSUE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_VERTICALSCALE_ISSUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceMetadataInstanceIdUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class StackUpscaleServiceTest {

    @Mock
    private StackScalabilityCondition stackScalabilityCondition;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetadataSetupService metadataSetupService;

    @InjectMocks
    private StackUpscaleService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private TransactionService transactionService;

    @Mock
    private InstanceMetadataInstanceIdUpdater instanceMetadataInstanceIdUpdater;

    private static CloudResource getCreatedInstanceResource(Long privateId, String instanceId, String groupName) {
        return CloudResource.builder()
                .withType(ResourceType.AWS_INSTANCE)
                .withPrivateId(privateId)
                .withInstanceId(instanceId)
                .withGroup(groupName)
                .withName(groupName + privateId)
                .withParameters(Map.of())
                .build();
    }

    @Test
    public void testGetInstanceCountToCreateWhenRepair() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackDto.getId()).thenReturn(1L);
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(stackDto, "worker", 3, true);
        assertEquals(3, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenUpscale() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackDto.getId()).thenReturn(1L);
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(stackDto, "worker", 3, false);
        assertEquals(1, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenStackIsNotScalable() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackDto.getId()).thenReturn(1L);
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(stackDto, "worker", 3, false);
        assertEquals(0, instanceCountToCreate);

    }

    private InstanceTemplate getInstanceTemplate(long privateId, String group) {
        return new InstanceTemplate("large", group, privateId, new ArrayList<>(), com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED, null, 1L,
                "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }

    @Test
    public void testBestEffort() throws QuotaExceededException, TransactionExecutionException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()))
                .thenReturn(Collections.emptyList());
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 0L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        underTest.upscale(mock(AuthenticatedContext.class), upscaleStackRequest, connector);
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(2)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
        List<CloudStack> cloudStacks = cloudStackArgumentCaptor.getAllValues();
        List<CloudInstance> cloudInstances = cloudStacks.get(1).getGroups().stream()
                .flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesWithoutInstanceId = cloudInstances.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() == null).collect(Collectors.toList());
        assertEquals(4, cloudInstances.size());
        assertEquals(2, cloudInstancesWithoutInstanceId.size());
    }

    @Test
    public void testBestEffortButFail() throws QuotaExceededException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 36, 40, "quota error", new Exception()));
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, 0L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        assertThrows(CloudConnectorException.class, () -> underTest.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
                connector));
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
    }

    @Test
    public void testExactButFailBecauseHigherThanProvisionable() throws QuotaExceededException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()));
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 3L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        assertThrows(CloudConnectorException.class, () -> underTest.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
                connector));
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
    }

    @Test
    public void testExactWithProvisionableCount() throws QuotaExceededException, TransactionExecutionException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        List<CloudResourceStatus> cloudResourceStatuses = List.of();
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()))
                .thenReturn(cloudResourceStatuses);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 2L);
        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();
        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class, Answers.RETURNS_DEEP_STUBS);

        underTest.upscale(authenticatedContext, upscaleStackRequest, connector);

        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(2)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
        List<CloudStack> cloudStacks = cloudStackArgumentCaptor.getAllValues();
        List<CloudInstance> cloudInstances = cloudStacks.get(1).getGroups().stream()
                .flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesWithoutInstanceId = cloudInstances.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() == null).collect(Collectors.toList());
        assertEquals(4, cloudInstances.size());
        assertEquals(2, cloudInstancesWithoutInstanceId.size());

        verify(instanceMetadataInstanceIdUpdater, times(1))
                .updateWithInstanceIdAndStatus(authenticatedContext, connector, cloudResourceStatuses);
    }

    @Test
    public void testPercentageButFailBecauseHigherThanProvisionable() throws QuotaExceededException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()));
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.PERCENTAGE, 60L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        assertThrows(CloudConnectorException.class, () -> underTest.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
                connector));
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
    }

    @Test
    public void testPercentageAndProvisionable() throws QuotaExceededException, TransactionExecutionException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()))
                .thenReturn(Collections.emptyList());
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.PERCENTAGE, 50L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        underTest.upscale(mock(AuthenticatedContext.class), upscaleStackRequest, connector);
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(2)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
        List<CloudStack> cloudStacks = cloudStackArgumentCaptor.getAllValues();
        List<CloudInstance> cloudInstances = cloudStacks.get(1).getGroups().stream()
                .flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesWithoutInstanceId = cloudInstances.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() == null).collect(Collectors.toList());
        assertEquals(4, cloudInstances.size());
        assertEquals(2, cloudInstancesWithoutInstanceId.size());
    }

    @Test
    void testVerticalScale() throws Exception {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        underTest.verticalScale(mock(AuthenticatedContext.class), new CoreVerticalScaleRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                mock(CloudStack.class), new ArrayList<>(), new StackVerticalScaleV4Request()), cloudConnector, "master");
        verify(resourceConnector, times(1)).update(any(), any(), any(), eq(UpdateType.VERTICAL_SCALE), eq(Optional.of("master")));
    }

    @Test
    void testVerticalScaleWithoutInstances() throws Exception {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        underTest.verticalScaleWithoutInstances(mock(AuthenticatedContext.class), new CoreVerticalScaleRequest<>(mock(CloudContext.class),
                mock(CloudCredential.class), mock(CloudStack.class), new ArrayList<>(), new StackVerticalScaleV4Request()), cloudConnector, "master");
        verify(resourceConnector, times(1)).update(any(), any(), any(), eq(UpdateType.VERTICAL_SCALE_WITHOUT_INSTANCES), eq(Optional.of("master")));
    }

    @Test
    void testVerticalScaleWithRollingVerticalScaleInstancesRequest() throws Exception {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        List<CloudResource> cloudResources = new ArrayList<>();
        CloudStack cloudStack = mock(CloudStack.class);
        RollingVerticalScaleInstancesRequest request = new RollingVerticalScaleInstancesRequest(
                1L,
                mock(CloudContext.class),
                mock(CloudCredential.class),
                cloudStack,
                cloudResources,
                new StackVerticalScaleV4Request(),
                mock(RollingVerticalScaleResult.class));
        underTest.verticalScale(mock(AuthenticatedContext.class), request, cloudConnector, "master");
        verify(resourceConnector, times(1)).update(any(), eq(cloudStack), eq(cloudResources), eq(UpdateType.VERTICAL_SCALE), eq(Optional.of("master")));
    }

    @Test
    void testVerticalScaleWithRollingVerticalScaleInstancesRequestWithException() throws Exception {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        List<CloudResource> cloudResources = new ArrayList<>();
        CloudStack cloudStack = mock(CloudStack.class);
        String errorMessage = "Test exception";
        Exception testException = new Exception(errorMessage);
        when(resourceConnector.update(any(), any(), any(), any(), any()))
                .thenThrow(testException)
                .thenReturn(Collections.emptyList());
        RollingVerticalScaleInstancesRequest request = new RollingVerticalScaleInstancesRequest(
                1L,
                mock(CloudContext.class),
                mock(CloudCredential.class),
                cloudStack,
                cloudResources,
                new StackVerticalScaleV4Request(),
                mock(RollingVerticalScaleResult.class));
        underTest.verticalScale(mock(AuthenticatedContext.class), request, cloudConnector, "master");
        verify(resourceConnector, times(2)).update(any(), eq(cloudStack), eq(cloudResources), eq(UpdateType.VERTICAL_SCALE), eq(Optional.of("master")));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(1L), eq(UPDATE_IN_PROGRESS.name()), eq(STACK_VERTICALSCALE_ISSUE), eq(errorMessage));
    }

    @Test
    void testGetInstanceStorageInfo() throws Exception {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        InstanceStoreMetadata resultMetadata = new InstanceStoreMetadata();
        when(metadataCollector.collectInstanceStorageCount(ac, List.of("m5d.2xlarge"))).thenReturn(resultMetadata);
        InstanceStoreMetadata instanceStoreMetadata = underTest.getInstanceStorageInfo(ac, "m5d.2xlarge", cloudConnector);
        verify(metadataCollector, times(1)).collectInstanceStorageCount(eq(ac), eq(List.of("m5d.2xlarge")));
        assertEquals(resultMetadata, instanceStoreMetadata);
    }

    @Test
    void testFinishInstances() {
        ArrayList<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        CloudResource cloudResourceMock = mock(CloudResource.class);
        when(cloudResourceMock.getType()).thenReturn(ResourceType.AWS_INSTANCE);
        cloudResourceStatuses.add(new CloudResourceStatus(cloudResourceMock, ResourceStatus.CREATED, 10L));
        cloudResourceStatuses.add(new CloudResourceStatus(cloudResourceMock, ResourceStatus.FAILED, 11L));
        cloudResourceStatuses.add(new CloudResourceStatus(cloudResourceMock, ResourceStatus.CREATED, 12L));
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        when(stackScalingFlowContext.getHostGroupWithAdjustment()).thenReturn(Map.of("worker", 3));
        when(stackScalingFlowContext.getStackId()).thenReturn(1L);
        underTest.finishAddInstances(stackScalingFlowContext, new UpscaleStackResult(1L, ResourceStatus.CREATED, cloudResourceStatuses));
        ArgumentCaptor<Set<String>> hostGroupCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<Long>> privateIdCaptor = ArgumentCaptor.forClass(Set.class);
        verify(metadataSetupService, times(1)).cleanupRequestedInstancesIfNotInList(eq(1L), hostGroupCaptor.capture(),
                privateIdCaptor.capture());
        assertThat(hostGroupCaptor.getValue()).containsExactly("worker");
        assertThat(privateIdCaptor.getValue()).containsExactly(10L, 12L);
    }

    @Test
    void testUpscaleShouldUpdateInstanceIdInInstanceMetadata() throws TransactionExecutionException, QuotaExceededException {
        long stackId = 1L;
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any()))
                .thenReturn(List.of());

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 2L);
        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();
        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        groups.add(Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(workerInstances)
                .build());
        List<CloudInstance> computeInstances = new ArrayList<>();
        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        groups.add(Group.builder()
                .withName("compute")
                .withType(InstanceGroupType.CORE)
                .withInstances(computeInstances)
                .build());

        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(mock(Network.class))
                .image(mock(Image.class))
                .instanceAuthentication(mock(InstanceAuthentication.class))
                .template("template")
                .fileSystem(mock(SpiFileSystem.class))
                .build();
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold, false);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        underTest.upscale(authenticatedContext, upscaleStackRequest, connector);

        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
        List<CloudStack> cloudStacks = cloudStackArgumentCaptor.getAllValues();
        List<CloudInstance> cloudInstances = cloudStacks.get(0).getGroups().stream()
                .flatMap(group -> group.getInstances().stream()).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesWithoutInstanceId = cloudInstances.stream()
                .filter(cloudInstance -> cloudInstance.getInstanceId() == null).collect(Collectors.toList());
        assertEquals(2, cloudInstances.size());
        assertEquals(0, cloudInstancesWithoutInstanceId.size());
        verify(instanceMetadataInstanceIdUpdater, times(1)).updateWithInstanceIdAndStatus(any(), any(), any());
    }

    @Test
    void testHandleUpscaleFailureWhenRepair() {
        underTest.handleStackUpscaleFailure(true, new HashMap<>(), new RuntimeException("error"), 1L, new HashMap<>());
        verify(metadataSetupService, times(1)).handleRepairFail(eq(1L), any());
        verify(metadataSetupService, never()).cleanupRequestedInstancesWithoutFQDN(anyLong(), any());
        verify(flowMessageService, times(1)).fireEventAndLog(eq(1L), eq(UPDATE_FAILED.name()), eq(STACK_REPAIR_FAILED), eq("error"));
    }

    @Test
    void testHandleUpscaleFailureWhenNotRepair() {
        underTest.handleStackUpscaleFailure(false, new HashMap<>(), new RuntimeException("error"), 1L, new HashMap<>());
        verify(metadataSetupService, times(1)).cleanupRequestedInstancesWithoutFQDN(eq(1L), any());
        verify(metadataSetupService, never()).handleRepairFail(anyLong(), any());
        verify(flowMessageService, times(1)).fireEventAndLog(eq(1L), eq(UPDATE_FAILED.name()), eq(STACK_INFRASTRUCTURE_UPDATE_FAILED), eq("error"));
    }
}