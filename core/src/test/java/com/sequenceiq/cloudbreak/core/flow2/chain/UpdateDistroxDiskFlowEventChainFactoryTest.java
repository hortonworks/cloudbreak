package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.FULL_START_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXDiskUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

class UpdateDistroxDiskFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP = "master";

    private static final String VOLUME_TYPE = "gp3";

    private static final int SIZE = 200;

    private static final String DISK_TYPE = DiskType.DATABASE_DISK.name();

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String ACCOUNT_ID = "test-account";

    @InjectMocks
    private UpdateDistroxDiskFlowEventChainFactory underTest;

    @BeforeEach
    void setUp() {
        underTest = new UpdateDistroxDiskFlowEventChainFactory();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitEvent() {
        assertEquals(DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueueForNonGcpDoesNotStopAndStartStack() {
        DistroXDiskUpdateTriggerEvent triggerEvent = triggerEvent(CloudPlatform.AWS.name(), true);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);
        assertEquals(triggerEvent, flowTriggerEventQueue.getTriggerEvent());

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(5, queue.size());
        assertInstanceOf(FlowChainInitPayload.class, queue.remove());
        assertSaltUpdateEvent(queue.remove());
        assertDistroXDiskUpdateEvent(queue.remove());
        assertDiskResizeEvent(queue.remove());
        assertInstanceOf(FlowChainFinalizePayload.class, queue.remove());
    }

    @Test
    void testCreateFlowTriggerEventQueueForGcpDiskTypeChangeStopsAndStartsStack() {
        DistroXDiskUpdateTriggerEvent triggerEvent = triggerEvent(CloudPlatform.GCP.name(), true);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);
        assertEquals(triggerEvent, flowTriggerEventQueue.getTriggerEvent());

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(7, queue.size());
        assertInstanceOf(FlowChainInitPayload.class, queue.remove());
        assertSaltUpdateEvent(queue.remove());
        assertStackEvent(queue.remove(), FULL_STOP_TRIGGER_EVENT);
        assertDistroXDiskUpdateEvent(queue.remove());
        assertStackEvent(queue.remove(), FULL_START_TRIGGER_EVENT);
        assertDiskResizeEvent(queue.remove());
        assertInstanceOf(FlowChainFinalizePayload.class, queue.remove());
    }

    @Test
    void testCreateFlowTriggerEventQueueForGcpSizeOnlyChangeDoesNotStopAndStartStack() {
        DistroXDiskUpdateTriggerEvent triggerEvent = triggerEvent(CloudPlatform.GCP.name(), false);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(5, queue.size());
        assertInstanceOf(FlowChainInitPayload.class, queue.remove());
        assertSaltUpdateEvent(queue.remove());
        assertDistroXDiskUpdateEvent(queue.remove());
        assertDiskResizeEvent(queue.remove());
        assertInstanceOf(FlowChainFinalizePayload.class, queue.remove());
    }

    private DistroXDiskUpdateTriggerEvent triggerEvent(String cloudPlatform, boolean diskTypeChangeRequested) {
        return DistroXDiskUpdateTriggerEvent.builder()
                .withSelector(DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT)
                .withResourceId(STACK_ID)
                .withClusterName(CLUSTER_NAME)
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform(cloudPlatform)
                .withStackId(STACK_ID)
                .withVolumeType(VOLUME_TYPE)
                .withSize(SIZE)
                .withGroup(GROUP)
                .withDiskType(DISK_TYPE)
                .withDiskTypeChangeRequested(diskTypeChangeRequested)
                .build();
    }

    private void assertSaltUpdateEvent(Selectable event) {
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.selector());
        assertInstanceOf(StackEvent.class, event);
        assertEquals(STACK_ID, event.getResourceId());
    }

    private void assertStackEvent(Selectable event, String selector) {
        assertEquals(selector, event.selector());
        assertInstanceOf(StackEvent.class, event);
        assertEquals(STACK_ID, event.getResourceId());
    }

    private void assertDiskResizeEvent(Selectable event) {
        assertEquals(DISK_RESIZE_TRIGGER_EVENT.event(), event.selector());
        assertInstanceOf(DiskResizeRequest.class, event);
        DiskResizeRequest diskResizeRequest = (DiskResizeRequest) event;
        assertEquals(STACK_ID, diskResizeRequest.getResourceId());
        assertEquals(GROUP, diskResizeRequest.getInstanceGroup());
    }

    private void assertDistroXDiskUpdateEvent(Selectable event) {
        assertEquals(DATAHUB_DISK_UPDATE_VALIDATION_EVENT.selector(), event.selector());
        assertInstanceOf(DistroXDiskUpdateEvent.class, event);
        DistroXDiskUpdateEvent diskUpdateEvent = (DistroXDiskUpdateEvent) event;
        assertEquals(STACK_ID, diskUpdateEvent.getResourceId());
        assertEquals(STACK_ID, diskUpdateEvent.getStackId());
        assertEquals(GROUP, diskUpdateEvent.getGroup());
        assertEquals(VOLUME_TYPE, diskUpdateEvent.getVolumeType());
        assertEquals(SIZE, diskUpdateEvent.getSize());
        assertEquals(DISK_TYPE, diskUpdateEvent.getDiskType());
        assertEquals(CLUSTER_NAME, diskUpdateEvent.getClusterName());
        assertEquals(ACCOUNT_ID, diskUpdateEvent.getAccountId());
    }
}
