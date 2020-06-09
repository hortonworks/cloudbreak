package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BackupRestoreStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupContext.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    public void backupDatabase(long stackId, String backupLocation) {
        LOGGER.info("HER BackupRestoreStatusService.backupDatabase");
    }

    public void backupDatabaseFinished(long stackId) {
        LOGGER.info("HER BackupRestoreStatusService.backupDatabaseFinished");
    }

    public void handleDatabaseBackupFailure(long stackId, String errorReason, DetailedStackStatus detailedStatus) {

    }
}
