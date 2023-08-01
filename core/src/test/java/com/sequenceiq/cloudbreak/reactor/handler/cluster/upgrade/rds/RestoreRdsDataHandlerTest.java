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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RestoreRdsDataHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @Mock
    private UpgradeRdsService upgradeRdsService;

    @Mock
    private HandlerEvent<UpgradeRdsDataRestoreRequest> event;

    @InjectMocks
    private RestoreRdsDataHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSDATARESTOREREQUEST");
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        UpgradeRdsDataRestoreRequest request = new UpgradeRdsDataRestoreRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).restoreRds(STACK_ID, request.getVersion().getMajorVersion());
        assertThat(result.selector()).isEqualTo("UPGRADERDSDATARESTORERESULT");
    }

    @Test
    void orchestrationException() throws CloudbreakOrchestratorException {
        UpgradeRdsDataRestoreRequest request = new UpgradeRdsDataRestoreRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(upgradeRdsService).restoreRds(
                eq(STACK_ID), eq(request.getVersion().getMajorVersion()));

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).restoreRds(STACK_ID, request.getVersion().getMajorVersion());
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
        assertThat(result).isInstanceOf(UpgradeRdsFailedEvent.class);
        UpgradeRdsFailedEvent failedEvent = (UpgradeRdsFailedEvent) result;
        assertThat(failedEvent.getException().getMessage()).isEqualTo("salt error");
    }
}