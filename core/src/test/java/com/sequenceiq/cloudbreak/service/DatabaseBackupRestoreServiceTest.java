package com.sequenceiq.cloudbreak.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.dr.BackupRestoreV4RequestValidator;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatabaseBackupRestoreServiceTest {

    private static final String CLUSTER_NAME = "cluster-name";

    private static final String CLUSTER_CRN = "cluster-crn";

    private static final long WORKSPACE_ID = 0L;

    private static final String BACKUP_ACTIVE_FLOW_EXCEPTION_MESSAGE = "Database backup cannot be performed because " +
        "there is an active flow running: ";

    private static final String RESTORE_ACTIVE_FLOW_EXCEPTION_MESSAGE = "Database restore cannot be performed because " +
        "there is an active flow running: ";

    private static final String MISSING_PARAM_EXCEPTION_MESSAGE = "Missing param";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSuccessfulBackup() {
        Stack stack = getStack();

        when(stackService.findStackByNameAndWorkspaceId(any(), anyLong())).thenReturn(Optional.of(stack));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(1L)).thenReturn(Collections.EMPTY_LIST);
        when(flowManager.triggerDatalakeDatabaseBackup(anyLong(), any(), any())).thenReturn(FlowIdentifier.notTriggered());

        service.backupDatabase(WORKSPACE_ID, ofName, null, null);
    }

    @Test
    public void testSuccessfulRestore() {
        Stack stack = getStack();

        when(stackService.findStackByNameAndWorkspaceId(any(), anyLong())).thenReturn(Optional.of(stack));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(1L)).thenReturn(Collections.EMPTY_LIST);
        when(flowManager.triggerDatalakeDatabaseRestore(anyLong(), any(), any())).thenReturn(FlowIdentifier.notTriggered());

        service.restoreDatabase(WORKSPACE_ID, ofName, null, null);
    }

    @Test
    public void testValidationSuccess() {
        Stack stack = getStack();

        when(stackService.findStackByNameAndWorkspaceId(any(), anyLong())).thenReturn(Optional.of(stack));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(requestValidator.validate(any(), any(), any())).thenReturn(getValidationResult(null));

        service.validate(WORKSPACE_ID, ofName, null, null);
    }

    @Test
    public void testValidationFailure() {
        Stack stack = getStack();

        when(stackService.findStackByNameAndWorkspaceId(any(), anyLong())).thenReturn(Optional.of(stack));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);
        when(requestValidator.validate(any(), any(), any())).thenReturn(getValidationResult(MISSING_PARAM_EXCEPTION_MESSAGE));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(MISSING_PARAM_EXCEPTION_MESSAGE);

        service.validate(WORKSPACE_ID, ofName, null, null);
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
