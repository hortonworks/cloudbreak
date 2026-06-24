package com.sequenceiq.redbeams.flow.redbeams.upgrade.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;

class RedbeamsUpgradeFlowEventChainFactoryTest {

    private static final Long RESOURCE_ID = 1L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    private RedbeamsUpgradeFlowEventChainFactory underTest;

    @BeforeEach
    void setUp() {
        underTest = new RedbeamsUpgradeFlowEventChainFactory();
    }

    @Test
    void testInitEvent() {
        assertEquals(EventSelectorUtil.selector(RedbeamsUpgradeFlowChainTriggerEvent.class), underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueueOrderAndSelectors() {
        RedbeamsUpgradeFlowChainTriggerEvent triggerEvent = createTriggerEvent();

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);
        List<Selectable> events = List.copyOf(eventQueue.getQueue());

        assertEquals(3, events.size());

        Selectable validate = events.get(0);
        assertInstanceOf(RedbeamsStartValidateUpgradeRequest.class, validate);
        assertEquals(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector(), validate.selector());

        Selectable cleanup = events.get(1);
        assertInstanceOf(RedbeamsStartValidateUpgradeCleanupRequest.class, cleanup);
        assertEquals(RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT.selector(), cleanup.selector());

        Selectable upgrade = events.get(2);
        assertInstanceOf(RedbeamsStartUpgradeRequest.class, upgrade);
        assertEquals(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), upgrade.selector());

        events.forEach(event -> assertEquals(RESOURCE_ID, event.getResourceId()));
    }

    @Test
    void testCreateFlowTriggerEventQueuePropagatesUpgradeParams() {
        RedbeamsUpgradeFlowChainTriggerEvent triggerEvent = createTriggerEvent();

        Queue<Selectable> queue = underTest.createFlowTriggerEventQueue(triggerEvent).getQueue();
        List<Selectable> events = List.copyOf(queue);

        RedbeamsStartValidateUpgradeRequest validate = (RedbeamsStartValidateUpgradeRequest) events.get(0);
        assertEquals(TARGET_MAJOR_VERSION, validate.getTargetMajorVersion());
        assertEquals(triggerEvent.getMigrationParams(), validate.getMigrationParams());

        RedbeamsStartUpgradeRequest upgrade = (RedbeamsStartUpgradeRequest) events.get(2);
        assertEquals(TARGET_MAJOR_VERSION, upgrade.getTargetMajorVersion());
        assertEquals(triggerEvent.getMigrationParams(), upgrade.getMigrationParams());
    }

    @Test
    void testFirstEventForwardsAcceptedPromise() {
        RedbeamsUpgradeFlowChainTriggerEvent triggerEvent = createTriggerEvent();
        Promise<AcceptResult> accepted = triggerEvent.accepted();

        List<Selectable> events = List.copyOf(underTest.createFlowTriggerEventQueue(triggerEvent).getQueue());

        assertSame(accepted, ((RedbeamsStartValidateUpgradeRequest) events.get(0)).accepted());
    }

    private RedbeamsUpgradeFlowChainTriggerEvent createTriggerEvent() {
        return new RedbeamsUpgradeFlowChainTriggerEvent(underTest.initEvent(), RESOURCE_ID, TARGET_MAJOR_VERSION,
                new UpgradeDatabaseMigrationParams());
    }
}
