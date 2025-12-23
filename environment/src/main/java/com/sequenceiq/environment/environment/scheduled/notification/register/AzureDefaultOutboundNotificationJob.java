package com.sequenceiq.environment.environment.scheduled.notification.register;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.environment.environment.service.notification.EnvironmentNotificationService;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.domain.Subscription;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.scheduled.register.AbstractScheduledNotificationJob;
import com.sequenceiq.notification.service.NotificationSendingService;

@Service
public class AzureDefaultOutboundNotificationJob
        extends AbstractScheduledNotificationJob {

    private static final String JOB_NAME = "azure-default-outbound-environment-notification";

    @Value("${notification.register.azuredefaultoutbound.enabled:true}")
    private boolean registerEnabled;

    private final EnvironmentNotificationService notificationService;

    public AzureDefaultOutboundNotificationJob(
            EnvironmentNotificationService notificationService,
            TransactionalScheduler scheduler,
            NotificationSendingService notificationSendingService) {
        super(scheduler, notificationSendingService);
        this.notificationService = notificationService;
    }

    @Override
    protected String getName() {
        return JOB_NAME;
    }

    @Override
    protected boolean enabled() {
        return registerEnabled;
    }

    @Override
    protected Collection<NotificationGeneratorDto> data() {
        return notificationService.filterForOutboundUpgradeNotifications();
    }

    @Override
    protected NotificationType notificationType() {
        return NotificationType.AZURE_DEFAULT_OUTBOUND;
    }

    @Override
    protected void processNotifications(NotificationGeneratorDtos notificationData) {
        getNotificationSendingService().processAndImmediatelySendWithCallback(notificationData,
                result -> onSubscriptionsProcessed(result.subscriptions()));
    }

    @Override
    public void onSubscriptionsProcessed(List<? extends Subscription> subscriptions) {
        List<DistributionList> distributionLists = getDistributionLists(subscriptions);
        notificationService.processDistributionListSync(subscriptions, distributionLists);
    }

    private List<DistributionList> getDistributionLists(List<? extends Subscription> subscriptions) {
        return subscriptions.stream()
                .filter(DistributionList.class::isInstance)
                .map(DistributionList.class::cast)
                .toList();
    }
}
