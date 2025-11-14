package com.sequenceiq.freeipa.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.metrics.MetricsClient;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltSyncService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StackStatusTest.TestAppContext.class)
class StackStatusTest {

    private static final Long STACK_ID = 123L;

    private static final String INSTANCE_1 = "i1";

    private static final String INSTANCE_2 = "i2";

    private static final String INSTANCE_3 = "i3";

    @Inject
    private StackStatusCheckerJob underTest;

    @MockBean
    private AutoSyncConfig autoSyncConfig;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @MockBean
    private SaltSyncService saltSyncService;

    @MockBean
    private StackService stackService;

    @MockBean
    private FlowLogService flowLogService;

    @MockBean
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    @MockBean
    private StackInstanceProviderChecker stackInstanceProviderChecker;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private FreeipaStatusInfoLogger freeipaStatusInfoLogger;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private MetricsClient metricsClient;

    @MockBean
    private StatusCheckerJobService jobService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private Stack stack;

    private Set<InstanceMetaData> notTerminatedInstances;

    private RPCResponse<Boolean> rpcResponse;

    void setUp(int instanceCount) throws Exception {
        underTest.setLocalId(STACK_ID.toString());

        stack = new Stack();
        stack.setId(STACK_ID);
        setStackSatus(DetailedStackStatus.PROVISIONED);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);

        InstanceGroup instanceGroup1 = new InstanceGroup();
        Set<InstanceMetaData> instances = new HashSet<>();
        if (instanceCount >= 1) {
            instances.add(createInstance(INSTANCE_1, "10.0.0.1"));
        }
        if (instanceCount >= 2) {
            instances.add(createInstance(INSTANCE_2, "10.0.0.2"));
        }
        if (instanceCount >= 3) {
            instances.add(createInstance(INSTANCE_3, "10.0.0.3"));
        }
        instanceGroup1.setInstanceMetaData(instances);
        stack.setInstanceGroups(Set.of(instanceGroup1));
        notTerminatedInstances = instances;
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(notTerminatedInstances);

        rpcResponse = new RPCResponse<>();
        when(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(eq(stack), any())).thenReturn(rpcResponse);

        lenient().when(autoSyncConfig.isSaltCheckEnabled()).thenReturn(Boolean.FALSE);
        lenient().when(autoSyncConfig.isUpdateStatus()).thenReturn(Boolean.TRUE);
        lenient().when(autoSyncConfig.isEnabled()).thenReturn(Boolean.TRUE);
    }

    private InstanceMetaData createInstance(String instanceName, String ip) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceId(instanceName);
        instanceMetaData.setDiscoveryFQDN(instanceName);
        instanceMetaData.setPrivateIp(ip);
        return instanceMetaData;
    }

    private void setStackSatus(DetailedStackStatus available) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(available);
        stack.setStackStatus(stackStatus);
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is available but salt check fails for it " +
                    "THEN stack status should change"
    )
    void saltCheckFailed() throws Exception {
        setUp(1);
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        when(autoSyncConfig.isSaltCheckEnabled()).thenReturn(Boolean.TRUE);
        when(autoSyncConfig.isSaltCheckStatusChangeEnabled()).thenReturn(Boolean.TRUE);
        when(saltSyncService.checkSaltMinions(any())).thenReturn(Optional.of(Set.of(INSTANCE_1)));
        setStackSatus(DetailedStackStatus.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), any(), any());
        verify(saltSyncService).checkSaltMinions(any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is available, salt check fails for it but status change is not enabled for salt failure " +
                    "THEN stack status should not change"
    )
    void saltCheckFailedStatusChangeNotEnabled() throws Exception {
        setUp(1);
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        when(autoSyncConfig.isSaltCheckEnabled()).thenReturn(Boolean.TRUE);
        when(autoSyncConfig.isSaltCheckStatusChangeEnabled()).thenReturn(Boolean.FALSE);
        when(saltSyncService.checkSaltMinions(any())).thenReturn(Optional.of(Set.of(INSTANCE_1)));
        setStackSatus(DetailedStackStatus.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater, never()).updateStackStatus(eq(stack), any(), any());
        verify(saltSyncService).checkSaltMinions(any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is available " +
                    "THEN stack status should not change"
    )
    void available() throws Exception {
        setUp(1);
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        setStackSatus(DetailedStackStatus.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater, never()).updateStackStatus(eq(stack), any(), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is available another is requested without IP and FQDN but with instance id" +
                    "THEN stack status should change to unhealthy and instance updated to failed"
    )
    void availableToUnhealthyForRequestedInstance() throws Exception {
        setUp(1);
        InstanceGroup instanceGroup1 = new InstanceGroup();
        Set<InstanceMetaData> instances = new HashSet<>();
        InstanceMetaData instance1 = createInstance(INSTANCE_1, "10.0.0.1");
        instances.add(instance1);
        InstanceMetaData instance2 = createInstance(INSTANCE_2, "10.0.0.2");
        instance2.setInstanceStatus(InstanceStatus.REQUESTED);
        instance2.setPrivateIp(null);
        instance2.setDiscoveryFQDN(null);
        instances.add(instance2);
        instanceGroup1.setInstanceMetaData(instances);
        stack.setInstanceGroups(Set.of(instanceGroup1));
        notTerminatedInstances = instances;
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(notTerminatedInstances);
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, Set.of(instance1))).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        setStackSatus(DetailedStackStatus.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UNHEALTHY), any());
        assertEquals(InstanceStatus.FAILED, instance2.getInstanceStatus());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is available another is requested without IP, FQDN and instance id" +
                    "THEN stack status should change to unhealthy and instance updated to failed"
    )
    void availableForRequestedInstanceWithoutId() throws Exception {
        setUp(1);
        InstanceGroup instanceGroup1 = new InstanceGroup();
        Set<InstanceMetaData> instances = new HashSet<>();
        InstanceMetaData instance1 = createInstance(INSTANCE_1, "10.0.0.1");
        instances.add(instance1);
        InstanceMetaData instance2 = createInstance(INSTANCE_2, "10.0.0.2");
        instance2.setInstanceStatus(InstanceStatus.REQUESTED);
        instance2.setPrivateIp(null);
        instance2.setDiscoveryFQDN(null);
        instance2.setInstanceId(null);
        instances.add(instance2);
        instanceGroup1.setInstanceMetaData(instances);
        stack.setInstanceGroups(Set.of(instanceGroup1));
        notTerminatedInstances = instances;
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(notTerminatedInstances);
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, Set.of(instance1))).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        setStackSatus(DetailedStackStatus.AVAILABLE);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(stackUpdater);
        assertEquals(InstanceStatus.TERMINATED, instance2.getInstanceStatus());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is deleted " +
                    "THEN stack status should change"
    )
    void deleted() throws Exception {
        setUp(1);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER)
        ));
        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is stopped " +
                    "THEN stack status should change"
    )
    void stoppped() throws Exception {
        setUp(1);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED)
        ));
        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.STOPPED), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN All FreeIpa instance are stopped " +
                    "THEN stack status should change"
    )
    void allStoppped() throws Exception {
        setUp(2);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED)
        ));
        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.STOPPED), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN All FreeIpa instance are stopped " +
                    "THEN stack status should change"
    )
    void someStoppped() throws Exception {
        setUp(2);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED)
        ));
        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UNHEALTHY), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instance is stopped, deleted, and available " +
                    "THEN stack status should change"
    )
    void unhealthy() throws Exception {
        setUp(3);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STOPPED),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER),
                createCloudVmInstanceStatus(INSTANCE_3, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));
        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UNHEALTHY), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                    "WHEN FreeIpa instances are terminated on provider " +
                    "THEN stack status should change to deleted on provider"
    )
    void updateToDeletedOnProvider() throws Exception {
        setUp(2);
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER)
        ));
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(Boolean.FALSE);

        underTest.executeJob(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE), any());
    }

    @Test
    @DisplayName(
            "GIVEN a deleteed on provider stack  with short sync" +
                    "WHEN FreeIpa instance are deleted on provider " +
                    "THEN stack status shouldn't change, but sync should switch to long interval"
    )
    void switchToLongSync() throws Exception {
        setUp(2);
        setUpFreeIpaAvailabilityResponse(false);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE);
        stackStatus.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        stack.setStackStatus(stackStatus);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER)
        ));
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(Boolean.FALSE);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(stackUpdater);
        verify(jobService).scheduleLongIntervalCheck(eq(STACK_ID), any());
        verify(jobService).unschedule(STACK_ID.toString());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack with long sync" +
                    "WHEN FreeIpa instances are available " +
                    "THEN stack status shouldn't change and switch to short sync"
    )
    void switchToShortSync() throws Exception {
        setUp(2);
        setUpFreeIpaAvailabilityResponse(false);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(DetailedStackStatus.AVAILABLE);
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED),
                createCloudVmInstanceStatus(INSTANCE_2, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED)
        ));
        when(jobService.isLongSyncJob(jobExecutionContext)).thenReturn(Boolean.TRUE);

        underTest.executeJob(jobExecutionContext);

        verifyNoInteractions(stackUpdater);
        verify(jobService).schedule(eq(STACK_ID), any());
        verify(jobService).unschedule(STACK_ID.toString());
    }

    private void setUpFreeIpaAvailabilityResponse(boolean value) {
        rpcResponse.setResult(value);
        RPCMessage mess = new RPCMessage();
        mess.setName("name");
        mess.setMessage("message");
        rpcResponse.setMessages(List.of(mess));
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(String instanceId, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus) {
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null, "subnet-1", "az1"), instanceStatus);
    }

    @Configuration
    @Import({
            StackStatusCheckerJob.class,
            FreeipaChecker.class,
            ProviderChecker.class,
            AutoSyncConfig.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {
    }
}
