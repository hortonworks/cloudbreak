package com.sequenceiq.notification.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.notification.config.NotificationConfig;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.CentralNotificationGeneratorService;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.scheduled.register.converter.NotificationGeneratorDtoToNotificationConverter;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingResult;

@Service
public class NotificationSendingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSendingService.class);

    private final NotificationDataAccessService notificationService;

    private final NotificationSender notificationSender;

    private final NotificationConfig config;

    private final NotificationGeneratorDtoToNotificationConverter baseNotificationViewToNotificationConverter;

    private final CentralNotificationGeneratorService centralNotificationGeneratorService;

    public NotificationSendingService(
            NotificationDataAccessService notificationService,
            NotificationSender notificationSender,
            NotificationGeneratorDtoToNotificationConverter baseNotificationViewToNotificationConverter,
            CentralNotificationGeneratorService centralNotificationGeneratorService,
            NotificationConfig config) {
        this.notificationService = notificationService;
        this.notificationSender = notificationSender;
        this.baseNotificationViewToNotificationConverter = baseNotificationViewToNotificationConverter;
        this.centralNotificationGeneratorService = centralNotificationGeneratorService;
        this.config = config;
    }

    public <T extends BaseNotificationRegisterAdditionalDataDtos> List<Notification> registerAllNotification(NotificationGeneratorDtos<T> dtos) {
        List<Notification> generatedNotifications = dtos.getNotifications()
                .stream()
                .map(dto -> centralNotificationGeneratorService.generateNotification(dto, dtos.getNotificationType()))
                .flatMap(generatorDto -> baseNotificationViewToNotificationConverter.convert(generatorDto, dtos.getNotificationType()).stream())
                .toList();

        Set<Notification> unsentNotifications = notificationService.collectUnsentNotifications(generatedNotifications);

        Map<String, Notification> generatedByKey = generatedNotifications.stream()
                .collect(Collectors.toMap(
                        this::createNotificationKey,
                        n -> n,
                        (existing, replacement) -> replacement
                ));

        List<Notification> result = new ArrayList<>();

        processGeneratedNotifications(generatedNotifications, result);
        processUnsentNotGeneratedNotifications(unsentNotifications, generatedByKey, result);

        return result;
    }

    private void processUnsentNotGeneratedNotifications(Set<Notification> unsentNotifications, Map<String, Notification> generatedByKey,
            List<Notification> result) {
        unsentNotifications.stream()
                .filter(unsent -> !generatedByKey.containsKey(createNotificationKey(unsent)))
                .forEach(unsent -> {
                    LOGGER.debug("Using unsent notification for resourceCrn {} and type {}",
                            unsent.getResourceCrn(), unsent.getType());
                    result.add(unsent);
                });
    }

    private void processGeneratedNotifications(List<Notification> generatedNotifications, List<Notification> result) {
        generatedNotifications.forEach(generated -> {
            LOGGER.debug("Creating notification for resourceCrn {} and type {}",
                    generated.getResourceCrn(), generated.getType());
            result.add(createNotificationEntity(generated));
        });
    }

    /**
     * Creates a composite key from a Notification's resource CRN and type.
     * This can be used for deduplication and lookup operations.
     *
     * @param notification the notification to create a key for
     * @return a composite key string
     */
    private String createNotificationKey(Notification notification) {
        return createNotificationKey(notification.getResourceCrn(), notification.getType());
    }

    /**
     * Creates a composite key from the given components.
     * This method can be extended to support additional grouping criteria in the future.
     *
     * @param components the components to create a key from
     * @return a composite key string with components joined by ":"
     */
    private String createNotificationKey(Object... components) {
        return String.join(":", java.util.Arrays.stream(components)
                .map(String::valueOf)
                .toArray(String[]::new));
    }

    public void processAndImmediatelySend(NotificationGeneratorDtos notificationGeneratorDtos) {
        processAndSend(registerAllNotification(notificationGeneratorDtos));
    }

    /**
     * Process and immediately send notifications with subscription callback support.
     *
     * @param notificationGeneratorDtos the notification data to process
     * @param subscriptionCallback callback to receive subscription information
     */
    public void processAndImmediatelySendWithCallback(NotificationGeneratorDtos notificationGeneratorDtos,
            Consumer<NotificationSendingResult> subscriptionCallback) {
        processAndSend(registerAllNotification(notificationGeneratorDtos), subscriptionCallback);
    }

    public void processAndSend(Collection<Notification> notificationsToProcess) {
        processAndSend(notificationsToProcess, null);
    }

    /**
     * Process and send notifications with subscription callback support.
     * This method allows jobs to receive subscription information after notifications are sent.
     *
     * @param notificationsToProcess the notifications to process and send
     * @param subscriptionCallback optional callback to receive subscription information
     */
    public void processAndSend(Collection<Notification> notificationsToProcess, Consumer<NotificationSendingResult> subscriptionCallback) {
        if (!CollectionUtils.isEmpty(notificationsToProcess)) {
            for (Entry<NotificationType, List<Notification>> entry : groupByNotificationType(notificationsToProcess)) {
                for (Entry<String, List<Notification>> resourceEntries : groupByResourceCrn(entry)) {
                    processAndSendForResource(resourceEntries.getValue(), subscriptionCallback);
                }
            }
        }
    }

    private void processAndSendForResource(List<Notification> notifications, Consumer<NotificationSendingResult> subscriptionCallback) {
        LOGGER.debug("Sending {} notifications", notifications.size());
        List<Notification> targets = notifications
                .stream()
                .filter(n -> config.isEnabled(Crn.fromString(n.getResourceCrn())))
                .toList();

        NotificationSendingResult result = notificationSender.sendNotifications(targets);

        Set<Long> notificationSentOut = result.notifications()
                .stream()
                .map(NotificationDto::getId)
                .collect(Collectors.toSet());

        notificationService.markAllAsSent(targets
                .stream()
                .map(Notification::getId)
                .filter(notificationSentOut::contains)
                .collect(toList())
        );

        if (subscriptionCallback != null) {
            subscriptionCallback.accept(result);
        }
    }

    private Set<Entry<NotificationType, List<Notification>>> groupByNotificationType(Collection<Notification> notificationsToProcess) {
        return notificationsToProcess
                .stream()
                .collect(groupingBy(Notification::getType))
                .entrySet();
    }

    private Set<Entry<String, List<Notification>>> groupByResourceCrn(Entry<NotificationType, List<Notification>> entry) {
        return entry.getValue()
                .stream()
                .collect(groupingBy(Notification::getResourceCrn))
                .entrySet();
    }

    private Notification createNotificationEntity(Notification missingRecordFromTheDatabase) {
        return notificationService.save(missingRecordFromTheDatabase);
    }
}
