package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupRestoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupRestoreService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ReactorFlowManager flowManager;

    public FlowIdentifier backupDatabase(Long workspaceId, String stackName, String location) {
        LOGGER.info("HER DatabaseBackupRestoreService.backupDatabase");
//        Optional<Stack> stackOptional = stackService.findStackByNameAndWorkspaceId(stackName, workspaceId);
//        if (stackOptional.isPresent()) {
//            Stack stack = stackOptional.get();
//            // TODO some kind of check to see if backup/restore flow is already running?
//            return flowManager.triggerDatalakeDatabaseBackup(stack.getId(), location);
//        } else {
//            throw notFoundException("Stack", stackName);
//        }
        return flowManager.triggerDatalakeDatabaseBackup(1L, location);
    }
}
