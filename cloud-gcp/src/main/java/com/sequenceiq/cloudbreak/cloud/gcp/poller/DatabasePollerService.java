package com.sequenceiq.cloudbreak.cloud.gcp.poller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Service
public class DatabasePollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabasePollerService.class);

    @Value("${cb.gcp.database.polling.attemptCount:360}")
    private Integer attemptCount;

    @Value("${cb.gcp.database.polling.sleep.time.seconds:5}")
    private Integer sleepTime;

    private GcpDatabasePollerProvider provider;

    public DatabasePollerService(GcpDatabasePollerProvider provider) {
        this.provider = provider;
    }

    public void launchDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        executeLaunchDatabasePollerPolling(provider.launchDatabasePoller(ac, resources), resources);
    }

    public void startDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        executeLaunchDatabasePollerPolling(provider.stopStartDatabasePoller(ac, resources), resources);
    }

    public void stopDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        executeLaunchDatabasePollerPolling(provider.stopStartDatabasePoller(ac, resources), resources);
    }

    public void insertUserPoller(AuthenticatedContext ac, List<CloudResource> resources) {
        executeUserInsertPollerPolling(provider.insertUserPoller(ac, resources), resources);
    }

    public void terminateDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        executeTerminateDatabasePollerPolling(provider.terminateDatabasePoller(ac, resources), resources);
    }

    private void executeUserInsertPollerPolling(AttemptMaker<Void> attemptMaker, List<CloudResource> resources) {
        if (CollectionUtils.isNotEmpty(resources)) {
            Polling.stopAfterAttempt(attemptCount)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(attemptMaker);
        }
    }

    private void executeLaunchDatabasePollerPolling(AttemptMaker<Void> attemptMaker, List<CloudResource> resources) {
        if (CollectionUtils.isNotEmpty(resources)) {
            Polling.stopAfterAttempt(attemptCount)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(attemptMaker);
        }
    }

    private void executeTerminateDatabasePollerPolling(AttemptMaker<Void> attemptMaker, List<CloudResource> resources) {
        if (CollectionUtils.isNotEmpty(resources)) {
            Polling.stopAfterAttempt(attemptCount)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(attemptMaker);
        }
    }
}
