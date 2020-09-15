package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.controller.validation.dr.BackupRestoreV4RequestValidator;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupRestoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupRestoreService.class);

    private static final String DATALAKE_DATABASE_BACKUP = "DATALAKE_DATABASE_BACKUP";

    private static final String DATALAKE_DATABASE_RESTORE = "DATALAKE_DATABASE_RESTORE";

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private BackupRestoreV4RequestValidator requestValidator;

    public void validate(Long workspaceId, NameOrCrn nameOrCrn, String location, String backupId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        ValidationResult validationResult = requestValidator.validate(stack, location, backupId);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    public FlowIdentifier backupDatabase(Long workspaceId, NameOrCrn nameOrCrn, String location,
            String backupId, String userCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Initiating database backup flow for stack {}", stack.getId());
        if (userCrn == null) {
            userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        }
        return flowManager.triggerDatalakeDatabaseBackup(stack.getId(), location, backupId, userCrn);
    }

    public FlowIdentifier restoreDatabase(Long workspaceId, NameOrCrn nameOrCrn, String location,
            String backupId, String userCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Initiating database restore flow for stack {}", stack.getId());
        if (userCrn == null) {
            userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        }
        return flowManager.triggerDatalakeDatabaseRestore(stack.getId(), location, backupId, userCrn);
    }
}
