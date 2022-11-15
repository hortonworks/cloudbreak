package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.DatabaseUpgradeBackupRestoreChecker;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ValidateRdsUpgradeServiceTest {

    private static final Long CLUSTER_ID = 42L;

    private static final Long STACK_ID = 24L;

    private static final String PUSH_SALT_STATE = "Pushing Salt states.";

    private static final String VALIDATE_BACKUP_STATE = "Validating if there is enough free space for backup during database server upgrade.";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private DatabaseUpgradeBackupRestoreChecker backupRestoreChecker;

    @Mock
    private StackView stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ValidateRdsUpgradeService underTest;

    @Test
    public void testValidateRdsUpgradeFinished() {
        underTest.validateRdsUpgradeFinished(STACK_ID, CLUSTER_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FINISHED),
                eq("Validate RDS upgrade finished"));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(AVAILABLE.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FINISHED));

    }

    @Test
    public void testValidateRdsUpgradeFailedWithException() {
        Exception exception = new RuntimeException("Could not determine database size.");
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any(Exception.class))).thenReturn(exception.getMessage());

        underTest.validateRdsUpgradeFailed(STACK_ID, CLUSTER_ID, exception);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED),
                eq("Validate RDS upgrade failed with exception: " + exception.getMessage()));
        verify(flowMessageService).fireEventAndLog(
                eq(STACK_ID),
                eq(UPDATE_FAILED.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FAILED),
                eq("Could not determine database size."));
    }

    @Test
    public void testShouldRunDataBackupRestore() {
        underTest.shouldRunDataBackupRestore(stack, cluster);

        verify(backupRestoreChecker).shouldRunDataBackupRestore(eq(stack), eq(cluster));
    }

    @Test
    public void testRdsUpgradeStarted() {
        underTest.rdsUpgradeStarted(STACK_ID, TargetMajorVersion.VERSION_11);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_RDS_UPGRADE_STARTED), eq("11"));    }

    @Test
    public void testPushSaltStatesState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES.getMessage())).thenReturn(PUSH_SALT_STATE);
        underTest.pushSaltStates(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS),
                eq(PUSH_SALT_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES));
    }

    @Test
    public void testValidateBackupState() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION.getMessage())).thenReturn(VALIDATE_BACKUP_STATE);
        underTest.validateBackup(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS),
                eq(VALIDATE_BACKUP_STATE));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION));
    }

    @Test
    public void testValidateOnCloudProvider() {
        when(messagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_ON_CLOUDPROVIDER.getMessage())).thenReturn("message");
        underTest.validateOnCloudProvider(STACK_ID);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS),
                eq("message"));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(UPDATE_IN_PROGRESS.name()),
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_ON_CLOUDPROVIDER));
    }
}
