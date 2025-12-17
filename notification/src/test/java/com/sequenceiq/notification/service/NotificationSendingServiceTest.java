package com.sequenceiq.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import com.sequenceiq.notification.sender.dto.NotificationSendingResult;

class NotificationSendingServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource-123";

    private static final String RESOURCE_CRN_2 = "crn:cdp:datahub:us-west-1:tenant:cluster:resource-456";

    private NotificationDataAccessService notificationService;

    private NotificationSender notificationSender;

    private NotificationConfig config;

    private NotificationGeneratorDtoToNotificationConverter baseNotificationViewToNotificationConverter;

    private CentralNotificationGeneratorService centralNotificationGeneratorService;

    private NotificationSendingService underTest;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationDataAccessService.class);
        notificationSender = mock(NotificationSender.class);
        config = mock(NotificationConfig.class);
        baseNotificationViewToNotificationConverter = mock(NotificationGeneratorDtoToNotificationConverter.class);
        centralNotificationGeneratorService = mock(CentralNotificationGeneratorService.class);

        underTest = new NotificationSendingService(
                notificationService,
                notificationSender,
                baseNotificationViewToNotificationConverter,
                centralNotificationGeneratorService,
                config
        );
    }

    @Test
    void registerAllNotificationWhenOnlyGeneratedExistsSavesAndReturnsGenerated() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();
        Notification generatedNotification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification savedNotification = createNotification(10L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of(generatedNotification));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of());
        when(notificationService.save(generatedNotification)).thenReturn(savedNotification);

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertEquals(1, result.size());
        assertEquals(savedNotification, result.getFirst());
        verify(notificationService).save(generatedNotification);
    }

    @Test
    void registerAllNotificationWhenOnlyUnsentExistsReturnsUnsentWithoutSaving() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();
        Notification unsentNotification = createNotification(2L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of());
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of(unsentNotification));

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertEquals(1, result.size());
        assertEquals(unsentNotification, result.getFirst());
        verify(notificationService, never()).save(unsentNotification);
    }

    @Test
    void registerAllNotificationWhenBothExistPrefersGeneratedAndSaves() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();
        Notification generatedNotification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification unsentNotification = createNotification(2L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification savedNotification = createNotification(10L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of(generatedNotification));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of(unsentNotification));
        when(notificationService.save(generatedNotification)).thenReturn(savedNotification);

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertEquals(1, result.size());
        assertEquals(savedNotification, result.getFirst());
        verify(notificationService).save(generatedNotification);
        verify(notificationService, never()).save(unsentNotification);
    }

    @Test
    void registerAllNotificationWithMultipleNotificationsProcessesAll() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();
        Notification generated1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification generated2 = createNotification(2L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification unsent1 = createNotification(3L, "crn:cdp:datahub:us-west-1:tenant:cluster:resource-789", NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification saved1 = createNotification(10L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification saved2 = createNotification(11L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of(generated1, generated2));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of(unsent1));
        when(notificationService.save(generated1)).thenReturn(saved1);
        when(notificationService.save(generated2)).thenReturn(saved2);

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertEquals(3, result.size());
        assertTrue(result.contains(saved1));
        assertTrue(result.contains(saved2));
        assertTrue(result.contains(unsent1));
        verify(notificationService, times(2)).save(any());
    }

    @Test
    void processAndImmediatelySendRegistersAndSendsNotifications() {
        NotificationGeneratorDtos notificationGeneratorDtos = createMockDtos();
        Notification notification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), any()))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), any()))
                .thenReturn(List.of(notification));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of());
        when(notificationService.save(any())).thenReturn(notification);
        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(List.of(NotificationDto.builder().id(1L).build()), List.of())
        );

        underTest.processAndImmediatelySend(notificationGeneratorDtos);

        verify(notificationSender).sendNotifications(anyList());
        verify(notificationService).markAllAsSent(anyList());
    }

    @Test
    void processAndImmediatelySendWithCallbackInvokesCallback() {
        NotificationGeneratorDtos notificationGeneratorDtos = createMockDtos();
        Notification notification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Consumer<NotificationSendingResult> callback = mock(Consumer.class);
        NotificationSendingResult sendingResult = new NotificationSendingResult(
                List.of(NotificationDto.builder().id(1L).build()), List.of()
        );

        when(centralNotificationGeneratorService.generateNotification(any(), any()))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), any()))
                .thenReturn(List.of(notification));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of());
        when(notificationService.save(any())).thenReturn(notification);
        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(sendingResult);

        underTest.processAndImmediatelySendWithCallback(notificationGeneratorDtos, callback);

        verify(callback).accept(sendingResult);
    }

    @Test
    void processAndSendWithEmptyCollectionDoesNothing() {
        underTest.processAndSend(List.of());

        verify(notificationSender, never()).sendNotifications(anyList());
        verify(notificationService, never()).markAllAsSent(anyList());
    }

    @Test
    void processAndSendWithNullCollectionDoesNothing() {
        underTest.processAndSend(null);

        verify(notificationSender, never()).sendNotifications(anyList());
        verify(notificationService, never()).markAllAsSent(anyList());
    }

    @Test
    void processAndSendGroupsNotificationsByTypeAndResource() {
        Notification notification1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification2 = createNotification(2L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification3 = createNotification(3L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(
                        List.of(
                                NotificationDto.builder().id(1L).build(),
                                NotificationDto.builder().id(2L).build(),
                                NotificationDto.builder().id(3L).build()
                        ),
                        List.of()
                )
        );

        underTest.processAndSend(List.of(notification1, notification2, notification3));

        verify(notificationSender, times(2)).sendNotifications(anyList());
    }

    @Test
    void processAndSendFiltersOutDisabledNotifications() {
        Notification enabledNotification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification disabledNotification = createNotification(2L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(config.isEnabled(Crn.fromString(RESOURCE_CRN))).thenReturn(true);
        when(config.isEnabled(Crn.fromString(RESOURCE_CRN_2))).thenReturn(false);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(List.of(NotificationDto.builder().id(1L).build()), List.of())
        );

        underTest.processAndSend(List.of(enabledNotification, disabledNotification));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender, times(2)).sendNotifications(captor.capture());

        List<List<Notification>> allInvocations = captor.getAllValues();
        long totalEnabledNotifications = allInvocations.stream()
                .mapToLong(List::size)
                .sum();
        assertEquals(1, totalEnabledNotifications);
        assertTrue(allInvocations.stream().anyMatch(List::isEmpty));
    }

    @Test
    void processAndSendMarksOnlySentNotificationsAsSent() {
        Notification notification1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification2 = createNotification(2L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(List.of(NotificationDto.builder().id(1L).build()), List.of())
        );

        underTest.processAndSend(List.of(notification1, notification2));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).markAllAsSent(captor.capture());

        List<Long> markedAsSent = captor.getValue();
        assertEquals(1, markedAsSent.size());
        assertTrue(markedAsSent.contains(1L));
    }

    @Test
    void processAndSendWithCallbackInvokesCallbackForEachResourceGroup() {
        Notification notification1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification2 = createNotification(2L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Consumer<NotificationSendingResult> callback = mock(Consumer.class);

        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(
                        List.of(NotificationDto.builder().id(1L).build()),
                        List.of()
                )
        );

        underTest.processAndSend(List.of(notification1, notification2), callback);

        verify(callback, times(2)).accept(any(NotificationSendingResult.class));
    }

    @Test
    void processAndSendWithoutCallbackDoesNotFail() {
        Notification notification = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(List.of(NotificationDto.builder().id(1L).build()), List.of())
        );

        underTest.processAndSend(List.of(notification), null);

        verify(notificationSender).sendNotifications(anyList());
    }

    @Test
    void registerAllNotificationHandlesDuplicateGeneratedNotifications() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();
        Notification generated1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification generated2 = createNotification(2L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification saved1 = createNotification(10L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification saved2 = createNotification(11L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of(generated1, generated2));
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of());
        when(notificationService.save(generated1)).thenReturn(saved1);
        when(notificationService.save(generated2)).thenReturn(saved2);

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertEquals(2, result.size());
        verify(notificationService, times(2)).save(any());
    }

    @Test
    void registerAllNotificationReturnsEmptyListWhenNoNotifications() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = createMockDtos();

        when(centralNotificationGeneratorService.generateNotification(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(mock(NotificationGeneratorDto.class));
        when(baseNotificationViewToNotificationConverter.convert(any(), eq(NotificationType.AZURE_DEFAULT_OUTBOUND)))
                .thenReturn(List.of());
        when(notificationService.collectUnsentNotifications(anyList())).thenReturn(Set.of());

        List<Notification> result = underTest.registerAllNotification(dtos);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void processAndSendGroupsByResourceCrnCorrectly() {
        Notification notification1 = createNotification(1L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification2 = createNotification(2L, RESOURCE_CRN_2, NotificationType.AZURE_DEFAULT_OUTBOUND);
        Notification notification3 = createNotification(3L, RESOURCE_CRN, NotificationType.AZURE_DEFAULT_OUTBOUND);

        when(config.isEnabled(any(Crn.class))).thenReturn(true);
        when(notificationSender.sendNotifications(anyList())).thenReturn(
                new NotificationSendingResult(
                        List.of(NotificationDto.builder().id(1L).build()),
                        List.of()
                )
        );

        underTest.processAndSend(List.of(notification1, notification2, notification3));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender, times(2)).sendNotifications(captor.capture());

        List<List<Notification>> allInvocations = captor.getAllValues();
        List<Integer> sizes = allInvocations.stream().map(List::size).sorted().toList();
        assertEquals(List.of(1, 2), sizes);
    }

    private Notification createNotification(Long id, String resourceCrn, NotificationType type) {
        return Notification.builder()
                .id(id)
                .resourceCrn(resourceCrn)
                .type(type)
                .name("Test Notification " + id)
                .build();
    }

    private NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> createMockDtos() {
        NotificationGeneratorDtos<BaseNotificationRegisterAdditionalDataDtos> dtos = mock(NotificationGeneratorDtos.class);
        when(dtos.getNotifications()).thenReturn(List.of(mock(NotificationGeneratorDto.class)));
        when(dtos.getNotificationType()).thenReturn(NotificationType.AZURE_DEFAULT_OUTBOUND);
        return dtos;
    }
}