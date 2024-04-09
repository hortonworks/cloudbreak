package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionRequest;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdatePostgresVersionHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @InjectMocks
    private UpdatePostgresVersionHandler underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Mock
    private HandlerEvent<UpgradeRdsUpdateVersionRequest> event;

    @Test
    void testSelector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSUPDATEVERSIONREQUEST");
    }

    @Test
    void testDoAccept() {
        UpgradeRdsUpdateVersionRequest request = new UpgradeRdsUpdateVersionRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(stackUpdater).updateExternalDatabaseEngineVersion(STACK_ID, TARGET_MAJOR_VERSION.getMajorVersion());
        verify(rdsUpgradeOrchestratorService).updateDatabaseEngineVersion(STACK_ID);
        assertThat(result.selector()).isEqualTo("UPGRADERDSUPDATEVERSIONRESULT");
    }

    @Test
    void testDoAcceptWhenOrchestratorServiceThrowsException() {
        UpgradeRdsUpdateVersionRequest request = new UpgradeRdsUpdateVersionRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakServiceException("error"))
                .when(rdsUpgradeOrchestratorService).updateDatabaseEngineVersion(STACK_ID);


        Selectable result = underTest.doAccept(event);
        verify(stackUpdater).updateExternalDatabaseEngineVersion(STACK_ID, TARGET_MAJOR_VERSION.getMajorVersion());
        verify(rdsUpgradeOrchestratorService).updateDatabaseEngineVersion(STACK_ID);
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
    }

}