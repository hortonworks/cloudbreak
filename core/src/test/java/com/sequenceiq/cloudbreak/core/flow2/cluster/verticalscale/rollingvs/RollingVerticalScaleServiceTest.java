package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ROOT_VOLUME_INCREASED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ROOT_VOLUME_INCREASING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTARTED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTARTING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_RESTART_INSTANCES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOPPED_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOPPING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_STOP_INSTANCES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_WAITING_FOR_SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALE_WAITING_FOR_SERVICES_HEALTHY_UNSUCCESSFUL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALING_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALING_INSTANCES_FAILED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP = "master";

    private static final String INSTANCE_ID_1 = "i-instance-1";

    private static final String INSTANCE_ID_2 = "i-instance-2";

    private static final List<String> INSTANCE_IDS = List.of(INSTANCE_ID_1, INSTANCE_ID_2);

    private static final String ERROR_MESSAGE = "Test error message";

    private static final String TARGET_INSTANCE_TYPE = "m5.2xlarge";

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private RollingVerticalScaleService underTest;

    @Test
    void testStopInstances() {
        underTest.stopInstances(STACK_ID, INSTANCE_IDS, GROUP);

        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(STACK_ID), eq(DetailedStackStatus.CLUSTER_VERTICALSCALE_IN_PROGRESS));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_STOPPING_INSTANCES), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
    }

    @Test
    void testFinishStopInstances() {
        underTest.finishStopInstances(STACK_ID, INSTANCE_IDS, GROUP);

        verify(instanceMetaDataService, times(1)).updateStatus(eq(STACK_ID), eq(INSTANCE_IDS), eq(InstanceStatus.STOPPED));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_STOPPED_INSTANCES), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
    }

    @Test
    void testFinishStopInstancesWithEmptyList() {
        underTest.finishStopInstances(STACK_ID, Collections.emptyList(), GROUP);

        verify(instanceMetaDataService, never()).updateStatus(any(), any(), any());
        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any());
    }

    @Test
    void testFailedToStopInstance() {
        underTest.failedToStopInstance(STACK_ID, INSTANCE_IDS, GROUP, ERROR_MESSAGE);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_STOP_INSTANCES_FAILED), eq(GROUP), eq(String.join(",", INSTANCE_IDS)), eq(ERROR_MESSAGE));
    }

    @Test
    void testFailedToStopInstanceWithEmptyList() {
        underTest.failedToStopInstance(STACK_ID, Collections.emptyList(), GROUP, ERROR_MESSAGE);

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testVerticalScaleInstancesWithInstanceType() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.verticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALING_INSTANCES),
                eq(GROUP),
                eq(String.join(", ", INSTANCE_IDS)));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASING), any(), any());
    }

    @Test
    void testVerticalScaleInstancesWithRootVolume() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(100);
        request.getTemplate().setRootVolume(rootVolume);

        underTest.verticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASING),
                eq("100"),
                eq(GROUP));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALING_INSTANCES), any(), any());
    }

    @Test
    void testVerticalScaleInstancesWithBothInstanceTypeAndRootVolume() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(100);
        request.getTemplate().setRootVolume(rootVolume);

        underTest.verticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALING_INSTANCES),
                eq(GROUP),
                eq(String.join(", ", INSTANCE_IDS)));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASING),
                eq("100"),
                eq(GROUP));
    }

    @Test
    void testVerticalScaleInstancesWithEmptyList() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.verticalScaleInstances(STACK_ID, Collections.emptyList(), request);

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any());
    }

    @Test
    void testFinishVerticalScaleInstancesWithInstanceType() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.finishVerticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(STOPPED.name()),
                eq(CLUSTER_VERTICALSCALED_INSTANCES),
                eq(GROUP),
                eq(String.join(", ", INSTANCE_IDS)));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASED), any(), any());
    }

    @Test
    void testFinishVerticalScaleInstancesWithRootVolume() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(100);
        request.getTemplate().setRootVolume(rootVolume);

        underTest.finishVerticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASED),
                eq("100"),
                eq(GROUP));
        verify(flowMessageService, never()).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALED_INSTANCES), any(), any());
    }

    @Test
    void testFinishVerticalScaleInstancesWithBothInstanceTypeAndRootVolume() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(100);
        request.getTemplate().setRootVolume(rootVolume);

        underTest.finishVerticalScaleInstances(STACK_ID, INSTANCE_IDS, request);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(STOPPED.name()),
                eq(CLUSTER_VERTICALSCALED_INSTANCES),
                eq(GROUP),
                eq(String.join(", ", INSTANCE_IDS)));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_ROOT_VOLUME_INCREASED),
                eq("100"),
                eq(GROUP));
    }

    @Test
    void testFinishVerticalScaleInstancesWithEmptyList() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.finishVerticalScaleInstances(STACK_ID, Collections.emptyList(), request);

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any());
    }

    @Test
    void testFailedVerticalScaleInstances() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.failedVerticalScaleInstances(STACK_ID, INSTANCE_IDS, request, ERROR_MESSAGE);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID),
                eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALING_INSTANCES_FAILED),
                eq(GROUP),
                eq(String.join(", ", INSTANCE_IDS)),
                eq(ERROR_MESSAGE));
    }

    @Test
    void testFailedVerticalScaleInstancesWithEmptyList() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();
        request.getTemplate().setInstanceType(TARGET_INSTANCE_TYPE);

        underTest.failedVerticalScaleInstances(STACK_ID, Collections.emptyList(), request, ERROR_MESSAGE);

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testFailedVerticalScaleInstancesWithoutInstanceType() {
        StackVerticalScaleV4Request request = createStackVerticalScaleV4Request();

        underTest.failedVerticalScaleInstances(STACK_ID, INSTANCE_IDS, request, ERROR_MESSAGE);

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testStartInstances() {
        underTest.startInstances(STACK_ID, INSTANCE_IDS, GROUP);

        verify(instanceMetaDataService, times(1)).updateStatus(eq(STACK_ID), eq(INSTANCE_IDS), eq(InstanceStatus.RESTARTING));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_RESTARTING_INSTANCES), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
    }

    @Test
    void testFinishStartInstances() {
        underTest.finishStartInstances(STACK_ID, INSTANCE_IDS, GROUP);

        verify(instanceMetaDataService, times(1)).updateStatus(eq(STACK_ID), eq(INSTANCE_IDS), eq(InstanceStatus.SERVICES_RUNNING));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_RESTARTED_INSTANCES), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
    }

    @Test
    void testFailedStartInstances() {
        underTest.failedStartInstances(STACK_ID, INSTANCE_IDS, GROUP, ERROR_MESSAGE);

        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_RESTART_INSTANCES_FAILED), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)), eq(ERROR_MESSAGE));
    }

    @Test
    void testFailedVerticalScale() {
        underTest.failedVerticalScale(STACK_ID, INSTANCE_IDS, ERROR_MESSAGE);

        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(STACK_ID), eq(DetailedStackStatus.CLUSTER_VERTICALSCALE_FAILED), eq(ERROR_MESSAGE));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(UPDATE_FAILED.name()),
                eq(CLUSTER_VERTICALSCALED_FAILED), eq(ERROR_MESSAGE), any(String.class));
    }

    @Test
    void testUpdateInstancesToServicesHealthyUpdatesStatuses() {
        InstanceMetadataView instance1 = mockInstanceMetadataView(INSTANCE_ID_1);
        InstanceMetadataView instance2 = mockInstanceMetadataView(INSTANCE_ID_2);
        Set<InstanceMetadataView> instances = new LinkedHashSet<>(List.of(instance1, instance2));

        underTest.updateInstancesToServicesHealthy(STACK_ID, instances);

        verify(instanceMetaDataService).updateStatus(eq(STACK_ID), eq(List.of(INSTANCE_ID_1, INSTANCE_ID_2)),
                eq(InstanceStatus.SERVICES_HEALTHY));
    }

    @Test
    void testUpdateInstancesToServicesHealthySkipsWhenEmpty() {
        underTest.updateInstancesToServicesHealthy(STACK_ID, Collections.emptySet());

        verify(instanceMetaDataService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void testUpdateInstancesToServiceUnhealthyUpdatesStatusesAndFiresEvent() {
        underTest.updateInstancesToServiceUnhealthy(STACK_ID, GROUP, INSTANCE_IDS);

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_FOR_SERVICES_HEALTHY_UNSUCCESSFUL), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
        verify(instanceMetaDataService).updateStatus(eq(STACK_ID), eq(INSTANCE_IDS), eq(InstanceStatus.SERVICES_UNHEALTHY));
    }

    @Test
    void testUpdateInstancesToServiceUnhealthySkipsWhenEmpty() {
        underTest.updateInstancesToServiceUnhealthy(STACK_ID, GROUP, Collections.emptyList());

        verify(flowMessageService, never()).fireEventAndLog(any(), any(), any(), any());
        verify(instanceMetaDataService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void testWaitingForServicesHealthyFiresEvent() {
        underTest.waitingForServicesHealthy(STACK_ID, GROUP, INSTANCE_IDS);

        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(CLUSTER_VERTICALSCALE_WAITING_FOR_SERVICES_HEALTHY), eq(GROUP), eq(String.join(", ", INSTANCE_IDS)));
    }

    private StackVerticalScaleV4Request createStackVerticalScaleV4Request() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(GROUP);
        request.setTemplate(new InstanceTemplateV4Request());
        return request;
    }

    private InstanceMetadataView mockInstanceMetadataView(String instanceId) {
        InstanceMetadataView metadataView = mock(InstanceMetadataView.class);
        when(metadataView.getInstanceId()).thenReturn(instanceId);
        return metadataView;
    }
}

