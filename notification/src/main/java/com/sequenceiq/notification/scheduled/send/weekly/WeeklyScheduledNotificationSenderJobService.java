package com.sequenceiq.notification.scheduled.send.weekly;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.scheduled.send.base.ScheduledBaseNotificationSenderJobService;
import com.sequenceiq.notification.service.NotificationSendingService;

@Service
public class WeeklyScheduledNotificationSenderJobService extends ScheduledBaseNotificationSenderJobService {

    @Value("${notification.sender.weekly.enabled:true}")
    private boolean enabled;

    private final NotificationDataAccessService notificationService;

    public WeeklyScheduledNotificationSenderJobService(
            TransactionalScheduler scheduler,
            NotificationSendingService sendingService,
            NotificationDataAccessService notificationService) {
        super(scheduler, sendingService);
        this.notificationService = notificationService;
    }

    @Override
    protected String periodName() {
        return "weekly";
    }

    @Override
    protected Class className() {
        return getClass();
    }

    @Override
    protected boolean enabled() {
        return enabled;
    }

    @Override
    protected Optional<String> cron() {
        return Optional.of("0 5 0 ? * SUN");
    }

    @Override
    protected Collection<Notification> data() {
        return notificationService.collectWeeklyEmailTargets();
    }

    @Override
    protected int intervalInHours() {
        return 0;
    }
}
