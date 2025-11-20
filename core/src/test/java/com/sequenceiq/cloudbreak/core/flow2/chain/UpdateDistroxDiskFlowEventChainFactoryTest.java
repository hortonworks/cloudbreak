package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXDiskUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

class UpdateDistroxDiskFlowEventChainFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP = "master";

    private static final String VOLUME_TYPE = "gp3";

    private static final int SIZE = 200;

    private static final String DISK_TYPE = DiskType.DATABASE_DISK.name();

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String ACCOUNT_ID = "test-account";

    private static final DistroXDiskUpdateTriggerEvent TRIGGER_EVENT = DistroXDiskUpdateTriggerEvent.builder()
            .withSelector(DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT)
            .withResourceId(STACK_ID)
            .withClusterName(CLUSTER_NAME)
            .withAccountId(ACCOUNT_ID)
            .withCloudPlatform(CloudPlatform.AWS.name())
            .withStackId(STACK_ID)
            .withVolumeType(VOLUME_TYPE)
            .withSize(SIZE)
            .withGroup(GROUP)
            .withDiskType(DISK_TYPE)
            .build();

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
    void testCreateFlowTriggerEventQueue() {
        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(TRIGGER_EVENT);
        assertEquals(TRIGGER_EVENT, flowTriggerEventQueue.getTriggerEvent());

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(2, queue.size());

        assertSaltUpdateEvent(queue.remove());
        assertDistroXDiskUpdateEvent(queue.remove());
    }

    private void assertSaltUpdateEvent(Selectable event) {
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.selector());
        assertInstanceOf(StackEvent.class, event);
        assertEquals(STACK_ID, event.getResourceId());
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
