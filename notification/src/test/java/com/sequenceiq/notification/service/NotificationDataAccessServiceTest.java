package com.sequenceiq.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.notification.config.NotificationConfig;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationDataAccessServiceTest {

    private NotificationConfig config;

    private NotificationRepository repository;

    private NotificationDataAccessService notificationService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        config = mock(NotificationConfig.class);
        repository = mock(NotificationRepository.class);
        transactionService = mock(TransactionService.class);
        notificationService = new NotificationDataAccessService(config, repository, transactionService);
    }

    @Test
    void testSendNotification() throws TransactionExecutionException {
        Notification notification = new Notification();
        notification.setMessage("Test message");

        notificationService.save(notification);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals("Test message", captor.getValue().getMessage());
    }

    @Test
    void saveReturnsNotificationWithCorrectMessage() throws TransactionExecutionException {
        Notification notification = new Notification();
        notification.setMessage("Sample message");

        when(repository.save(notification)).thenReturn(notification);

        Notification result = notificationService.save(notification);

        assertEquals("Sample message", result.getMessage());
    }

    @Test
    void collectWeeklyEmailTargetsReturnsEmptyWhenRepositoryReturnsNull() {
        when(repository.findDistinctResourceCrnBySentFalse()).thenReturn(new ArrayList<>());

        Collection<Notification> result = notificationService.collectWeeklyEmailTargets();

        assertTrue(result.isEmpty());
    }

    @Test
    void markAllAsSentDoesNotThrowExceptionWhenProcessedIdsIsEmpty() {
        List<Long> processedIds = List.of();

        notificationService.markAllAsSent(processedIds);

        verify(repository, never()).markAsSent(processedIds, System.currentTimeMillis());
    }

    @Test
    void hasNoExistingUnsentRecordByResourceCrnAndType() {
        String resourceCrn = "crn1";
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;
        Notification notification = new Notification();
        notification.setResourceCrn(resourceCrn);
        notification.setType(type);

        when(repository.collectExistingUnsentRecordByResourceCrnAndType(Set.of(resourceCrn), type)).thenReturn(Set.of());

        Set<Notification> notifications = notificationService.collectUnsentNotifications(List.of(notification));

        assertTrue(notifications.isEmpty());
    }

    @Test
    void hasExistingUnsentRecordByResourceCrnAndTypeReturnsTrueWhenRepositoryReturnsTrue() {
        String resourceCrn = "crn1";
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;
        Notification notification = new Notification();
        notification.setResourceCrn(resourceCrn);
        notification.setType(type);

        when(repository.collectExistingUnsentRecordByResourceCrnAndType(Set.of(resourceCrn), type)).thenReturn(Set.of(resourceCrn));

        Set<Notification> notifications = notificationService.collectUnsentNotifications(List.of(notification));

        assertEquals(1, notifications.size());
        Notification actualNotification = notifications.iterator().next();
        assertEquals(resourceCrn, actualNotification.getResourceCrn());
        assertEquals(type, actualNotification.getType());
    }

    @Test
    void alreadySentNotificationDeletion() {
        when(repository.purgeSentNotifications()).thenReturn(1);

        notificationService.deleteAllAlreadySentNotifications();

        verify(repository).purgeSentNotifications();
    }
}
