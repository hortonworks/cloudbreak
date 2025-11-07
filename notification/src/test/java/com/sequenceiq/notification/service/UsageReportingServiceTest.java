package com.sequenceiq.notification.service;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.NotificationType.Value.AZURE_DEFAULT_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.dto.NotificationDto;

@ExtendWith(MockitoExtension.class)
public class UsageReportingServiceTest {

    @InjectMocks
    private UsageReportingService underTest;

    @Mock
    private UsageReporter usageReporter;

    @Test
    public void testSentNotificationEvent() {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setAccountId("accountId");
        notificationDto.setSent(true);
        notificationDto.setSentAt(1212L);
        notificationDto.setName("test");
        notificationDto.setChannelType(ChannelType.EMAIL);
        notificationDto.setMessage("Testing the EDH event");
        notificationDto.setCreatedAt(1000L);
        notificationDto.setResourceCrn("testStackCrn");
        notificationDto.setResourceName("testStackName");
        notificationDto.setFormFactor(NotificationFormFactor.SUBSCRIPTION);
        notificationDto.setSeverity(NotificationSeverity.WARNING);
        notificationDto.setType(NotificationType.AZURE_DEFAULT_OUTBOUND);

        underTest.sendNotificationEvent(notificationDto);

        ArgumentCaptor<UsageProto.CDPNotificationSentEvent> captor = ArgumentCaptor.forClass(UsageProto.CDPNotificationSentEvent.class);
        verify(usageReporter, times(1)).cdpNotificationSentEvent(captor.capture());
        UsageProto.CDPNotificationSentEvent actual = captor.getValue();

        assertEquals("testStackName", actual.getResourceName(), "Stack Name should match");
        assertEquals("testStackCrn", actual.getResourceCrn(), "Stacl crn should match");
        assertEquals("Testing the EDH event", actual.getMessage(), "Message should match");
        assertEquals(AZURE_DEFAULT_OUTBOUND, actual.getNotificationType(), "Notification type should match");
    }

    @Test
    public void testSendNotificationEventWithNullFields() {
        // Create NotificationDto with only a few fields set and others null
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setAccountId("accountId");
        notificationDto.setSent(true);
        // Explicitly set one field null to confirm behavior
        notificationDto.setMessage(null);
        // All other fields implicitly remain null

        underTest.sendNotificationEvent(notificationDto);

        ArgumentCaptor<UsageProto.CDPNotificationSentEvent> captor =
                ArgumentCaptor.forClass(UsageProto.CDPNotificationSentEvent.class);
        verify(usageReporter, times(1)).cdpNotificationSentEvent(captor.capture());
        UsageProto.CDPNotificationSentEvent actual = captor.getValue();

        // Verify that only non-null fields were set
        assertEquals("accountId", actual.getAccountId());
        assertTrue(actual.getSent());

        // Verify no NPEs were thrown during processing of null fields
        // Note: Protocol Buffers will initialize unset fields with default values
        // No need to explicitly test those values as we're testing the null-handling
        // behavior, not the Protocol Buffer defaults
    }

}
