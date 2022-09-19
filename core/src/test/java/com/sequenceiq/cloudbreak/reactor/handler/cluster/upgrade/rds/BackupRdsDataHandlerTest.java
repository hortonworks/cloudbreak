package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class BackupRdsDataHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    private static final String BACKUP_LOCATION = "abfs://abcd@efgh.dfs.core.windows.net";

    @Mock
    private UpgradeRdsService upgradeRdsService;

    @Mock
    private HandlerEvent<UpgradeRdsDataBackupRequest> event;

    @InjectMocks
    private BackupRdsDataHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSDATABACKUPREQUEST");
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        UpgradeRdsDataBackupRequest request = new UpgradeRdsDataBackupRequest(STACK_ID, TARGET_MAJOR_VERSION, BACKUP_LOCATION);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).backupRds(STACK_ID, BACKUP_LOCATION);
        assertThat(result.selector()).isEqualTo("UPGRADERDSDATABACKUPRESULT");
    }

    @Test
    void orchestrationException() throws CloudbreakOrchestratorException {
        UpgradeRdsDataBackupRequest request = new UpgradeRdsDataBackupRequest(STACK_ID, TARGET_MAJOR_VERSION, BACKUP_LOCATION);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(upgradeRdsService).backupRds(eq(STACK_ID), eq(BACKUP_LOCATION));

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).backupRds(STACK_ID, BACKUP_LOCATION);
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
        assertThat(result).isInstanceOf(UpgradeRdsFailedEvent.class);
        UpgradeRdsFailedEvent failedEvent = (UpgradeRdsFailedEvent) result;
        assertThat(failedEvent.getException().getMessage()).isEqualTo("salt error");
    }
}