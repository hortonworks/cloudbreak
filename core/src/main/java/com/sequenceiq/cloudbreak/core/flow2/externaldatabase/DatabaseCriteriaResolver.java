package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static com.google.common.base.Preconditions.checkArgument;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Component
class DatabaseCriteriaResolver {

    @NotNull
    AttemptResult<Object> resolveResultByCriteria(DatabaseOperation databaseOperation, DatabaseServerV4Response rdsStatus, Cluster cluster) {
        checkArgument(databaseOperation != null, "DatabaseOperation should not be null");
        checkArgument(rdsStatus != null, "DatabaseServerV4Response should not be null");
        AttemptResult<Object> result;
        if (databaseOperation.getExitCriteria().apply(rdsStatus.getStatus())) {
            result = AttemptResults.finishWith(rdsStatus);
        } else {
            if (databaseOperation.getFailureCriteria().apply(rdsStatus.getStatus())) {
                result = rdsStatus.getStatusReason() != null && rdsStatus.getStatusReason().contains("does not exist")
                        ? AttemptResults.finishWith(null)
                        : AttemptResults.breakFor(getDatabaseOperationFailedResultMessage(cluster, rdsStatus));
            } else {
                result = AttemptResults.justContinue();
            }
        }
        return result;
    }

    private String getDatabaseOperationFailedResultMessage(Cluster cluster, DatabaseServerV4Response rdsStatus) {
        return String.format("Database operation failed on %s, statusReason: %s", cluster.getName(), rdsStatus.getStatusReason());
    }

}
