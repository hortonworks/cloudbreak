package com.sequenceiq.notification.scheduled.cleanup;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.notification.repository.NotificationDataAccessService;

@Component
public class NotificationCleanupJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCleanupJob.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private NotificationDataAccessService notificationService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            LOGGER.debug("Cleaning successful sent notifications");
            notificationService.deleteAllAlreadySentNotifications();
        } catch (Exception e) {
            LOGGER.error("Transaction failed for flow cleanup.", e);
            throw new JobExecutionException(e);
        }
    }
}
