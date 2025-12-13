package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.dr.BackupRestoreV4RequestValidator;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class DatabaseBackupRestoreServiceTest {

    private static final String CLUSTER_NAME = "cluster-name";

    private static final String CLUSTER_CRN = "cluster-crn";

    private static final long WORKSPACE_ID = 0L;

    private static final String BACKUP_ACTIVE_FLOW_EXCEPTION_MESSAGE = "Database backup cannot be performed because " +
        "there is an active flow running: ";

    private static final String RESTORE_ACTIVE_FLOW_EXCEPTION_MESSAGE = "Database restore cannot be performed because " +
        "there is an active flow running: ";

    private static final String MISSING_PARAM_EXCEPTION_MESSAGE = "Missing param";

    private final NameOrCrn ofName = NameOrCrn.ofName(CLUSTER_NAME);

    private final NameOrCrn ofCrn = NameOrCrn.ofName(CLUSTER_CRN);

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackService stackService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private BackupRestoreV4RequestValidator requestValidator;

    @InjectMocks
    private DatabaseBackupRestoreService service;

    @Test
    void testSuccessfulBackup() {
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(flowManager.triggerDatalakeDatabaseBackup(anyLong(), any(), any(), anyBoolean(), any(), eq(0), anyBoolean()))
            .thenReturn(FlowIdentifier.notTriggered());

        service.backupDatabase(WORKSPACE_ID, ofName, null, null, true, Collections.emptyList(), 0, false);
    }

    @Test
    void testSuccessfulRestore() {
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(flowManager.triggerDatalakeDatabaseRestore(anyLong(), any(), any(), eq(0), eq(false))).thenReturn(FlowIdentifier.notTriggered());

        service.restoreDatabase(WORKSPACE_ID, ofName, null, null, 0, false);
    }

    @Test
    void testValidationSuccess() {
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(requestValidator.validate(any(), any(), any())).thenReturn(getValidationResult(null));

        service.validate(WORKSPACE_ID, ofName, null, null);
    }

    @Test
    void testValidationFailure() {
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(requestValidator.validate(any(), any(), any())).thenReturn(getValidationResult(MISSING_PARAM_EXCEPTION_MESSAGE));

        assertThrows(BadRequestException.class, () -> service.validate(WORKSPACE_ID, ofName, null, null), MISSING_PARAM_EXCEPTION_MESSAGE);
    }

    @Test
    void testSuccessfulDatabaseBackupWithCustomizedMaxDurationInMin() {
        int databaseMaxDurationInMin = 20;
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(flowManager.triggerDatalakeDatabaseBackup(anyLong(), any(), any(), anyBoolean(), any(), eq(databaseMaxDurationInMin), anyBoolean()))
            .thenReturn(FlowIdentifier.notTriggered());

        service.backupDatabase(WORKSPACE_ID, ofName, null, null, true, Collections.emptyList(), databaseMaxDurationInMin, false);
    }

    @Test
    void testSuccessfulDatabaseRestoreWithCustomizedMaxDurationInMin() {
        int databaseMaxDurationInMin = 20;
        Stack stack = getStack();

        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);

        service.backupDatabase(WORKSPACE_ID, ofName, null, null, true, Collections.emptyList(), databaseMaxDurationInMin, false);
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn("env-crn");
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant("AWS");
        stack.setRegion("eu-central-1");
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        return stack;
    }

    private ValidationResult getValidationResult(String error) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (error != null) {
            resultBuilder.error(MISSING_PARAM_EXCEPTION_MESSAGE);
        }
        return  resultBuilder.build();
    }
}
