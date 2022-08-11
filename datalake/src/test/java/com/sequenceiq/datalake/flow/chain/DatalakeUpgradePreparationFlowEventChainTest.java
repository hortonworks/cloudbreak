package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradePreparationFlowChainStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

public class DatalakeUpgradePreparationFlowEventChainTest {
    private static final Long SDX_ID = 0L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String IMAGE_ID = "test_image_id";

    private static final String BACKUP_LOCATION = "s3a://test";

    @InjectMocks
    private DatalakeUpgradePreparationFlowEventChainFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void chainCreationTest() {
        DatalakeUpgradePreparationFlowChainStartEvent event = new DatalakeUpgradePreparationFlowChainStartEvent(
                SDX_ID, USER_CRN, IMAGE_ID, BACKUP_LOCATION
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(2, flowTriggerEventQueue.getQueue().size());

        Queue<Selectable> flowQueue = flowTriggerEventQueue.getQueue();
        checkEventIsBackupValidation(flowQueue.remove());
        checkEventIsUpgradePreparation(flowQueue.remove());
    }

    private void checkEventIsBackupValidation(Selectable event) {
        assertEquals(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT.selector(), event.selector());
        assertTrue(event instanceof DatalakeTriggerBackupValidationEvent);
        assertEquals(SDX_ID, event.getResourceId());
        DatalakeTriggerBackupValidationEvent backupValidationEvent = (DatalakeTriggerBackupValidationEvent) event;
        assertEquals(USER_CRN, backupValidationEvent.getUserId());
        assertEquals(BACKUP_LOCATION, backupValidationEvent.getBackupLocation());
        assertEquals(DatalakeBackupFailureReason.BACKUP_ON_UPGRADE, backupValidationEvent.getReason());
    }

    private void checkEventIsUpgradePreparation(Selectable event) {
        assertEquals(DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT.selector(), event.selector());
        assertTrue(event instanceof DatalakeUpgradePreparationStartEvent);
        assertEquals(SDX_ID, event.getResourceId());
        DatalakeUpgradePreparationStartEvent upgradePreparationStartEvent = (DatalakeUpgradePreparationStartEvent) event;
        assertEquals(USER_CRN, upgradePreparationStartEvent.getUserId());
        assertEquals(IMAGE_ID, upgradePreparationStartEvent.getImageId());
    }
}
