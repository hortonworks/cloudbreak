package com.sequenceiq.notification.sender.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.GrpcNotificationClient;
import com.sequenceiq.cloudbreak.notification.client.dto.PublishEventForResourceRequestDto;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.sender.NotificationSenderService;
import com.sequenceiq.notification.sender.converter.NotificationSeverityConverter;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.service.UsageReportingService;

@Service
public class EmailSenderService implements NotificationSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);

    private final GrpcNotificationClient grpcNotificationClient;

    private final NotificationSeverityConverter notificationSeverityConverter;

    private final UsageReportingService usageReportingService;

    public EmailSenderService(GrpcNotificationClient grpcNotificationClient,
            NotificationSeverityConverter notificationSeverityConverter, UsageReportingService usageReportingService) {
        this.grpcNotificationClient = grpcNotificationClient;
        this.notificationSeverityConverter = notificationSeverityConverter;
        this.usageReportingService = usageReportingService;
    }

    @Override
    public void send(NotificationDto notificationDto) {
        NotificationAdminProto.SeverityType.Value severity = notificationSeverityConverter.convert(notificationDto.getSeverity());
        PublishEventForResourceRequestDto request = new PublishEventForResourceRequestDto(notificationDto.getResourceCrn(),
                notificationDto.getType().getEventTypeId(),
                String.format(notificationDto.getType().getSubjectTemplate(), notificationDto.getName()),
                notificationDto.getMessage(),
                severity);
        LOGGER.debug("Sending {} email notification for resourceCrn: {} with type: {}",
                severity,
                notificationDto.getResourceCrn(),
                notificationDto.getType());
        grpcNotificationClient.publishEventForResource(request);
        usageReportingService.sendNotificationEvent(notificationDto);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.EMAIL;
    }
}
