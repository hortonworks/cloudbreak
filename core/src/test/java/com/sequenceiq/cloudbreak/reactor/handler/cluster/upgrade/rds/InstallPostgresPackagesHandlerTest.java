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

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class InstallPostgresPackagesHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @Mock
    private UpgradeRdsService upgradeRdsService;

    @Mock
    private HandlerEvent<UpgradeRdsInstallPostgresPackagesRequest> event;

    @InjectMocks
    private InstallPostgresPackagesHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSINSTALLPOSTGRESPACKAGESREQUEST");
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        UpgradeRdsInstallPostgresPackagesRequest request = new UpgradeRdsInstallPostgresPackagesRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).installPostgresPackages(STACK_ID, MajorVersion.VERSION_11);
        assertThat(result.selector()).isEqualTo("UPGRADERDSINSTALLPOSTGRESPACKAGESRESULT");
    }

    @Test
    void orchestrationException() throws CloudbreakOrchestratorException {
        UpgradeRdsInstallPostgresPackagesRequest request = new UpgradeRdsInstallPostgresPackagesRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakOrchestratorFailedException("salt error"))
                .when(upgradeRdsService).installPostgresPackages(eq(STACK_ID), eq(MajorVersion.VERSION_11));

        Selectable result = underTest.doAccept(event);
        verify(upgradeRdsService).installPostgresPackages(STACK_ID, MajorVersion.VERSION_11);
        assertThat(result.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
        assertThat(result).isInstanceOf(UpgradeRdsFailedEvent.class);
        verify(upgradeRdsService).handleInstallPostgresPackagesError(eq(STACK_ID), eq("11"), eq("salt error"));
    }

}