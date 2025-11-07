package com.sequenceiq.notification.sender.mail;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.GrpcNotificationClient;
import com.sequenceiq.cloudbreak.notification.client.dto.PublishEventForResourceRequestDto;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.converter.NotificationSeverityConverter;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.service.UsageReportingService;

class EmailSenderServiceTest {

    private GrpcNotificationClient grpcNotificationClient;

    private NotificationSeverityConverter notificationSeverityConverter;

    private UsageReportingService usageReportingService;

    private EmailSenderService emailSenderService;

    @BeforeEach
    void setUp() {
        grpcNotificationClient = mock(GrpcNotificationClient.class);
        notificationSeverityConverter = mock(NotificationSeverityConverter.class);
        usageReportingService = mock(UsageReportingService.class);
        emailSenderService = new EmailSenderService(grpcNotificationClient, notificationSeverityConverter, usageReportingService);
    }

    @Test
    void sendPublishesNotificationWithCorrectParameters() throws Exception {
        NotificationDto notificationDto = NotificationDto.builder()
                .notification(Notification.builder().build())
                .accountId("1")
                .message("message1")
                .name("name1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .channelType(ChannelType.EMAIL)
                .resourceCrn("crn1")
                .severity(NotificationSeverity.CRITICAL)
                .notificationFormFactor(NotificationFormFactor.SUBSCRIPTION)
                .build();
        NotificationAdminProto.SeverityType.Value severity = NotificationAdminProto.SeverityType.Value.CRITICAL;

        PublishEventForResourceRequestDto expectedRequest = new PublishEventForResourceRequestDto(
                "crn1",
                "b1417842-1eef-4d65-ac36-02a0e32d424e",
                "Cloudera Data Platform - Azure Default Outbound Warning: name1",
                "message1",
                severity
        );

        when(notificationSeverityConverter.convert(notificationDto.getSeverity())).thenReturn(severity);

        emailSenderService.send(notificationDto);

        verify(grpcNotificationClient).publishEventForResource(expectedRequest);
        verify(usageReportingService).sendNotificationEvent(notificationDto);
    }

    @Test
    void sendThrowsExceptionWhenGrpcClientFails() {
        NotificationDto notificationDto = NotificationDto.builder()
                .notification(Notification.builder().build())
                .accountId("1")
                .name("name1")
                .message("message1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .channelType(ChannelType.EMAIL)
                .resourceCrn("crn1")
                .severity(NotificationSeverity.CRITICAL)
                .notificationFormFactor(NotificationFormFactor.SUBSCRIPTION)
                .build();
        NotificationAdminProto.SeverityType.Value severity = NotificationAdminProto.SeverityType.Value.CRITICAL;

        when(notificationSeverityConverter.convert(notificationDto.getSeverity())).thenReturn(severity);
        doThrow(new RuntimeException("gRPC failure")).when(grpcNotificationClient).publishEventForResource(any(PublishEventForResourceRequestDto.class));

        assertThrows(RuntimeException.class, () -> emailSenderService.send(notificationDto));
    }

    @Test
    void channelTypeReturnsEmail() {
        ChannelType result = emailSenderService.channelType();

        assertThat(result).isEqualTo(ChannelType.EMAIL);
    }

}