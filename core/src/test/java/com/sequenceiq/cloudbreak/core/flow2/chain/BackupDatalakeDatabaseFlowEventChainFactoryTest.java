package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class BackupDatalakeDatabaseFlowEventChainFactoryTest {
    private static final Long STACK_ID = 1L;

    private static final String BACKUP_LOCATION = "s3a://test";

    private static final String BACKUP_ID = "backup-id";

    private static final Boolean CLOSE_CONNECTIONS = false;

    private static final Integer DATABASE_MAX_DURATION_IN_MIN = 0;

    private static final Boolean DRY_RUN = false;

    private static final List<String> SKIP_DB_NAMES = Collections.singletonList("atlas");

    private static final DatabaseBackupTriggerEvent TRIGGER_EVENT = new DatabaseBackupTriggerEvent(
            FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT, STACK_ID,
            BACKUP_LOCATION, BACKUP_ID, CLOSE_CONNECTIONS, SKIP_DB_NAMES, DATABASE_MAX_DURATION_IN_MIN, DRY_RUN
    );

    @Mock
    private StackService stackService;

    @InjectMocks
    private BackupDatalakeDatabaseFlowEventChainFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void chainCreationTestSaltUpdateNeeded() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(DetailedStackStatus.AVAILABLE);
        when(stackService.getCurrentStatusByStackId(eq(STACK_ID))).thenReturn(stackStatus);

        Queue<Selectable> flowQueue = factory.createFlowTriggerEventQueue(TRIGGER_EVENT).getQueue();
        assertEquals(2, flowQueue.size());

        checkEventIsSaltUpdate(flowQueue.remove());
        checkEventIsDatabaseBackup(flowQueue.remove());
    }

    @Test
    public void chainCreationTestSaltUpdateAlreadyCompleted() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED);
        when(stackService.getCurrentStatusByStackId(eq(STACK_ID))).thenReturn(stackStatus);

        Queue<Selectable> flowQueue = factory.createFlowTriggerEventQueue(TRIGGER_EVENT).getQueue();
        assertEquals(1, flowQueue.size());

        checkEventIsDatabaseBackup(flowQueue.remove());
    }

    private void checkEventIsSaltUpdate(Selectable event) {
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.selector());
        assertTrue(event instanceof StackEvent);
        assertEquals(STACK_ID, event.getResourceId());
    }

    private void checkEventIsDatabaseBackup(Selectable event) {
        assertEquals(DATABASE_BACKUP_EVENT.event(), event.selector());
        assertTrue(event instanceof DatabaseBackupTriggerEvent);
        assertEquals(STACK_ID, event.getResourceId());

        DatabaseBackupTriggerEvent databaseBackupTriggerEvent = (DatabaseBackupTriggerEvent) event;
        assertEquals(BACKUP_LOCATION, databaseBackupTriggerEvent.getBackupLocation());
        assertEquals(BACKUP_ID, databaseBackupTriggerEvent.getBackupId());
        assertEquals(CLOSE_CONNECTIONS, databaseBackupTriggerEvent.isCloseConnections());
        assertEquals(SKIP_DB_NAMES, databaseBackupTriggerEvent.getSkipDatabaseNames());
    }
}
