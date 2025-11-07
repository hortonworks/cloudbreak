package com.sequenceiq.notification.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.scheduled.register.converter.NotificationGeneratorDtoToNotificationConverter;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.sender.dto.NotificationDto;

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
        List<NotificationGeneratorDto> generatorDtos = dtos.getNotifications()
                .stream()
                .map(dto -> centralNotificationGeneratorService.generateNotification(dto, dtos.getNotificationType()))
                .toList();
        List<Notification> notifications = generatorDtos
                .stream()
                .map(e -> baseNotificationViewToNotificationConverter.convert(e, dtos.getNotificationType()))
                .flatMap(List::stream)
                .toList();
        List<Notification> result = new ArrayList<>();

        notificationService.collectUnsentNotifications(notifications).forEach(missingRecordsFromTheDatabase -> {
            LOGGER.debug("Creating notification for resourceCrn {} and type {}",
                    missingRecordsFromTheDatabase.getResourceCrn(),
                    missingRecordsFromTheDatabase.getType()
            );
            result.add(createNotificationEntity(missingRecordsFromTheDatabase));
        });

        return result;
    }

    public void processAndImmediatelySend(NotificationGeneratorDtos notificationGeneratorDtos) {
        processAndSend(registerAllNotification(notificationGeneratorDtos));
    }

    public void processAndSend(Collection<Notification> notificationsToProcess) {
        if (!CollectionUtils.isEmpty(notificationsToProcess)) {
            for (Entry<NotificationType, List<Notification>> entry : groupByNotificationType(notificationsToProcess)) {
                for (Entry<String, List<Notification>> resourceEntries : groupByResourceCrn(entry)) {
                    List<Notification> targets = resourceEntries.getValue()
                            .stream()
                            .filter(n -> config.isEnabled(Crn.fromString(n.getResourceCrn())))
                            .toList();

                    Set<Long> notificationSentOut = notificationSender.sendNotifications(targets)
                            .stream()
                            .map(NotificationDto::getId)
                            .collect(Collectors.toSet());
                    notificationService.markAllAsSent(targets
                            .stream()
                            .map(Notification::getId)
                            .filter(notificationSentOut::contains)
                            .collect(toList())
                    );
                }
            }
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
