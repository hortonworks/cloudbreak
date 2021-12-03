package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.TestUtil.instanceMetaData;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPSCALE_QUOTA_ISSUE;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;

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

    @InjectMocks
    private StackUpscaleService stackUpscaleService;

    @Test
    public void testGetInstanceCountToCreateWhenRepair() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = stackUpscaleService.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, true);
        assertEquals(3, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenUpscale() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = stackUpscaleService.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, false);
        assertEquals(1, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenStackIsNotScalable() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = stackUpscaleService.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, false);
        assertEquals(0, instanceCountToCreate);

    }

    private InstanceTemplate getInstanceTemplate(long privateId, String group) {
        return new InstanceTemplate("large", group, privateId, new ArrayList<>(), com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED, null, 1L,
                "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }

    @Test
    public void testBestEffort() throws QuotaExceededException {
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
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest, connector);
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
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        Assertions.assertThrows(CloudConnectorException.class, () -> stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
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
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        Assertions.assertThrows(CloudConnectorException.class, () -> stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
                connector));
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
    }

    @Test
    public void testExactWithProvisionableCount() throws QuotaExceededException {
        CloudConnector connector = mock(CloudConnector.class);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(connector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new QuotaExceededException(40, 20, 40, "quota error", new Exception()))
                .thenReturn(Collections.emptyList());

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 2L);

        List<Group> groups = new ArrayList<>();
        List<CloudInstance> workerInstances = new ArrayList<>();

        workerInstances.add(new CloudInstance("W1", getInstanceTemplate(1L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker1.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(2L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker2.example.com")));
        workerInstances.add(new CloudInstance(null, getInstanceTemplate(3L, "worker"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "worker3.example.com")));
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest, connector);
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
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        Assertions.assertThrows(CloudConnectorException.class, () -> stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest,
                connector));
        verify(flowMessageService, times(1)).fireEventAndLog(upscaleStackRequest.getResourceId(), UPDATE_IN_PROGRESS.name(), STACK_UPSCALE_QUOTA_ISSUE,
                "quota error");
        ArgumentCaptor<CloudStack> cloudStackArgumentCaptor = ArgumentCaptor.forClass(CloudStack.class);
        verify(resourceConnector, times(1)).upscale(any(), cloudStackArgumentCaptor.capture(), any(), eq(adjustmentTypeWithThreshold));
    }

    @Test
    public void testPercentageAndProvisionable() throws QuotaExceededException {
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
        groups.add(new Group("worker", InstanceGroupType.CORE, workerInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));
        List<CloudInstance> computeInstances = new ArrayList<>();

        computeInstances.add(new CloudInstance("C1", getInstanceTemplate(4L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute1.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(5L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute2.example.com")));
        computeInstances.add(new CloudInstance(null, getInstanceTemplate(6L, "compute"), mock(InstanceAuthentication.class), "subnet-1", "az1",
                Map.of(CloudInstance.FQDN, "compute3.example.com")));
        groups.add(new Group("compute", InstanceGroupType.CORE, computeInstances, mock(Security.class), mock(CloudInstance.class),
                mock(InstanceAuthentication.class), "admin", "ssh", 100, Optional.empty(), null, emptyMap()));

        CloudStack cloudStack = new CloudStack(groups, mock(Network.class), mock(Image.class), Collections.emptyMap(), Collections.emptyMap(), "template",
                mock(InstanceAuthentication.class), "username", "publickey", mock(SpiFileSystem.class));
        UpscaleStackRequest<UpscaleStackResult> upscaleStackRequest = new UpscaleStackRequest<>(mock(CloudContext.class), mock(CloudCredential.class),
                cloudStack, new ArrayList<>(), adjustmentTypeWithThreshold);
        stackUpscaleService.upscale(mock(AuthenticatedContext.class), upscaleStackRequest, connector);
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
}