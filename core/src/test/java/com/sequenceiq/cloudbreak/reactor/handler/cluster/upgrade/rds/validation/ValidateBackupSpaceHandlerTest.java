package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

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

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ValidateBackupSpaceHandlerTest {

    private static final Long STACK_ID = 42L;

    @Mock
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Mock
    private HandlerEvent<ValidateRdsUpgradeBackupValidationRequest> event;

    @InjectMocks
    private ValidateBackupSpaceHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("VALIDATERDSUPGRADEBACKUPVALIDATIONREQUEST");
    }

    @Test
    void doAccept() throws CloudbreakOrchestratorException {
        ValidateRdsUpgradeBackupValidationRequest request = new ValidateRdsUpgradeBackupValidationRequest(STACK_ID);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);
        verify(rdsUpgradeOrchestratorService).determineDbBackupLocation(STACK_ID);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEBACKUPVALIDATIONRESULT");
    }

    @Test
    void orchestrationException() throws CloudbreakOrchestratorException {
        ValidateRdsUpgradeBackupValidationRequest request = new ValidateRdsUpgradeBackupValidationRequest(STACK_ID);
        when(event.getData()).thenReturn(request);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(rdsUpgradeOrchestratorService).determineDbBackupLocation(eq(STACK_ID));

        Selectable result = underTest.doAccept(event);
        verify(rdsUpgradeOrchestratorService).determineDbBackupLocation(STACK_ID);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        ValidateRdsUpgradeFailedEvent failedEvent = (ValidateRdsUpgradeFailedEvent) result;
        assertThat(failedEvent.getException().getMessage()).isEqualTo("salt error");
    }
}
