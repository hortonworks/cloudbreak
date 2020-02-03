package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterPollingCheckerService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
class DatabaseObtainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseObtainerService.class);

    private final RedbeamsClientService redbeamsClient;

    private final ClusterPollingCheckerService clusterPollingCheckerService;

    private final DatabaseCriteriaResolver databaseCriteriaResolver;

    DatabaseObtainerService(RedbeamsClientService redbeamsClient, ClusterPollingCheckerService clusterPollingCheckerService,
            DatabaseCriteriaResolver databaseCriteriaResolver) {
        this.redbeamsClient = redbeamsClient;
        this.clusterPollingCheckerService = clusterPollingCheckerService;
        this.databaseCriteriaResolver = databaseCriteriaResolver;
    }

    AttemptResult<Object> obtainAttemptResult(Cluster cluster, DatabaseOperation databaseOperation, String databaseCrn, boolean cancellable)
            throws JsonProcessingException {

        Optional<AttemptResult<Object>> result = Optional.ofNullable(clusterPollingCheckerService.checkClusterCancelledState(cluster, cancellable));
        if (result.isEmpty()) {
            checkArgument(cluster != null, "Cluster must not be null");
            try {
                LOGGER.info("Creation polling redbeams for database status: '{}'", cluster.getName());
                DatabaseServerV4Response rdsStatus = redbeamsClient.getByCrn(databaseCrn);
                LOGGER.info("Response from redbeams: {}", JsonUtil.writeValueAsString(rdsStatus));
                result = Optional.of(databaseCriteriaResolver.resolveResultByCriteria(databaseOperation, rdsStatus, cluster));
            } catch (NotFoundException e) {
                result = Optional.of(AttemptResults.finishWith(null));
            }
        }
        return result.get();
    }

}
