package com.sequenceiq.notification.repository;

import static com.sequenceiq.notification.domain.ChannelType.EMAIL;
import static com.sequenceiq.notification.domain.NotificationType.getByChannelType;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.notification.config.NotificationConfig;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;

@Service
public class NotificationDataAccessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDataAccessService.class);

    private final NotificationConfig config;

    private final NotificationRepository repository;

    private final TransactionService transactionService;

    public NotificationDataAccessService(
            NotificationConfig config,
            NotificationRepository repository,
            TransactionService transactionService) {
        this.config = config;
        this.repository = repository;
        this.transactionService = transactionService;
    }

    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    public Collection<Notification> collectWeeklyEmailTargets() {
        List<Notification> result = new ArrayList<>();
        List<String> resources = repository.findDistinctResourceCrnBySentFalse();

        Map<String, List<String>> groupedByAccount = resources.stream()
                .collect(Collectors.groupingBy(resourceCrn -> Crn.safeFromString(resourceCrn).getAccountId()));
        for (Map.Entry<String, List<String>> entry : groupedByAccount.entrySet()) {
            if (config.isEnabledByAccountID(entry.getKey())) {
                for (String resourceCrn : entry.getValue()) {
                    List<Notification> notifications = repository
                            .findByResourceCrnAndTypeContainsTypesAndSentFalse(resourceCrn, getByChannelType(EMAIL))
                            .stream()
                            .limit(config.getBatchSize())
                            .toList();
                    result.addAll(notifications);
                    LOGGER.debug("Collected {} weekly email notifications for resourceCrn: {}", notifications.size(), resourceCrn);
                }
            }
        }
        LOGGER.debug("Collected {} weekly email notifications", result.size());
        return result;
    }

    public Set<Notification> collectUnsentNotifications(List<Notification> registrationTargets) {
        Set<Notification> missingRecordsFromTheDatabase = new HashSet<>();
        for (Map.Entry<NotificationType, List<Notification>> registrationTargetEntry : groupNotificationsByType(registrationTargets)) {
            missingRecordsFromTheDatabase.addAll(collectExistingUnsentNotificationsFromDatabaseForASpecificType(
                    registrationTargetEntry.getValue(),
                    collectExistingPairsInDB(registrationTargetEntry)
            ));
        }
        return missingRecordsFromTheDatabase;
    }

    private Set<Map.Entry<NotificationType, List<Notification>>> groupNotificationsByType(List<Notification> registrationTargets) {
        return registrationTargets.stream()
                .collect(groupingBy(Notification::getType))
                .entrySet();
    }

    private Set<String> collectExistingPairsInDB(Map.Entry<NotificationType, List<Notification>> registrationTargetsEntry) {
        return repository.collectExistingUnsentRecordByResourceCrnAndType(
                collectCrns(registrationTargetsEntry.getValue()),
                registrationTargetsEntry.getKey()
        );
    }

    private Set<Notification> collectExistingUnsentNotificationsFromDatabaseForASpecificType(List<Notification> entries, Set<String> existingPairsInDB) {
        return entries.stream().filter(n -> !existingPairsInDB.contains(n.getResourceCrn())).collect(Collectors.toSet());
    }

    private Set<String> collectCrns(List<Notification> registrationTargetCrns) {
        return registrationTargetCrns.stream().map(Notification::getResourceCrn).collect(Collectors.toSet());
    }

    public void markAllAsSent(List<Long> processedIds) {
        if (!CollectionUtils.isEmpty(processedIds)) {
            repository.markAsSent(processedIds, System.currentTimeMillis());
            LOGGER.debug("Marked {} notifications as sent", processedIds.size());
        }
    }

    public void deleteAllAlreadySentNotifications() {
        int deletedCount = repository.purgeSentNotifications();
        LOGGER.debug("Deleted {} sent notifications", deletedCount);
    }
}
