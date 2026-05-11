package com.sequenceiq.environment.environment.service.database;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;

@Service
public class RedbeamsPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerService.class);

    private final DatabaseServerV4Endpoint databaseServerV4Endpoint;

    private final RedbeamsPollerProvider redbeamsPollerProvider;

    @Value("${env.stack.config.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.stack.config.update.sleep.time.seconds:60}")
    private Integer sleepTime;

    public RedbeamsPollerService(
            DatabaseServerV4Endpoint databaseServerV4Endpoint,
            RedbeamsPollerProvider redbeamsPollerProvider) {
        this.databaseServerV4Endpoint = databaseServerV4Endpoint;
        this.redbeamsPollerProvider = redbeamsPollerProvider;
    }

    public List<FlowIdentifier> updateUserDefinedTagsOnDatabases(Long envId, String envCrn, Map<String, String> tags) {
        DatabaseServerV4Responses databaseServerV4Responses = databaseServerV4Endpoint.list(envCrn);
        List<String> dbCrns = databaseServerV4Responses.getResponses().stream()
                .map(DatabaseServerV4Response::getCrn)
                .toList();
        LOGGER.info("User defined tags will be updated on databases: {}", dbCrns);
        return startStackUserDefinedTagsUpdatePolling(dbCrns,
                redbeamsPollerProvider.userDefinedTagsUpdatePoller(dbCrns, envId, tags));
    }

    private List<FlowIdentifier> startStackUserDefinedTagsUpdatePolling(List<String> stackNames, AttemptMaker<List<FlowIdentifier>> attemptMaker) {
        if (CollectionUtils.isNotEmpty(stackNames)) {
            try {
                return Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.warn("DB stack user defined tags updating timed out");
                throw new StackOperationFailedException("DB stack user defined tags updating timed out", e);
            } catch (UserBreakException e) {
                LOGGER.error("DB stack user defined tags updating aborted with error", e);
                throw new StackOperationFailedException("DB stack user defined tags updating aborted with error", e);
            }
        }
        return Collections.emptyList();
    }
}
