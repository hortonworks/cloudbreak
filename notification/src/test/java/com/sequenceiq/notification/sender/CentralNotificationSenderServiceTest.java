package com.sequenceiq.notification.sender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.converter.NotificationDtosToCreateDistributionListRequestConverter;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;

class CentralNotificationSenderServiceTest {

    private NotificationSenderService notificationSenderService;

    private Map<ChannelType, NotificationSenderService> senderServiceMap;

    private DistributionListManagementService distributionListManagementService;

    private NotificationDtosToCreateDistributionListRequestConverter converter;

    private CentralNotificationSenderService underTest;

    @BeforeEach
    void setUp() {
        notificationSenderService = mock(NotificationSenderService.class);
        when(notificationSenderService.channelType()).thenReturn(ChannelType.EMAIL);
        senderServiceMap = Map.of(
                ChannelType.EMAIL, notificationSenderService
        );
        distributionListManagementService = mock(DistributionListManagementService.class);
        converter = mock(NotificationDtosToCreateDistributionListRequestConverter.class);
        underTest = new CentralNotificationSenderService(List.of(
                senderServiceMap.get(ChannelType.EMAIL)),
                distributionListManagementService,
                converter);
    }

    @Test
    void sendNotificationToDeliverySystemProcessesAllNotifications() throws Exception {
        NotificationDto notification1 = NotificationDto.builder()
                .message("message1")
                .resourceCrn("crn1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name1")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationDto notification2 = NotificationDto.builder()
                .message("message2")
                .resourceCrn("crn2")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name2")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(notification1, notification2));

        CreateDistributionListRequest request1 = getDistributionListRequest("crn1");
        CreateDistributionListRequest request2 = getDistributionListRequest("crn2");

        Set<CreateDistributionListRequest> resourceCrns = Set.of(request1, request2);

        when(distributionListManagementService.createOrUpdateLists(resourceCrns)).thenReturn(List.of());

        underTest.sendNotificationToDeliverySystem(dtos);

        verify(senderServiceMap.get(ChannelType.EMAIL)).send(notification1);
        verify(senderServiceMap.get(ChannelType.EMAIL)).send(notification2);
    }

    @Test
    void sendNotificationToDeliverySystemHandlesMissingSenderServiceGracefully() throws Exception {
        NotificationDto notification = NotificationDto.builder()
                .message("message1")
                .resourceCrn("crn1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name1")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(notification));

        CreateDistributionListRequest request1 = getDistributionListRequest("crn1");
        CreateDistributionListRequest request2 = getDistributionListRequest("crn2");

        Set<CreateDistributionListRequest> resourceCrns = Set.of(request1, request2);

        when(distributionListManagementService.createOrUpdateLists(resourceCrns)).thenReturn(List.of());

        underTest.sendNotificationToDeliverySystem(dtos);

        verify(notificationSenderService, times(1)).send(any());
    }

    @Test
    void sendNotificationToDeliverySystemLogsErrorWhenSenderServiceThrowsException() throws Exception {
        NotificationDto notification = NotificationDto.builder()
                .message("message1")
                .resourceCrn("crn1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name1")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(notification));

        CreateDistributionListRequest request1 = getDistributionListRequest("crn1");
        CreateDistributionListRequest request2 = getDistributionListRequest("crn2");

        Set<CreateDistributionListRequest> resourceCrns = Set.of(request1, request2);

        when(distributionListManagementService.createOrUpdateLists(resourceCrns)).thenReturn(List.of());
        doThrow(new RuntimeException("Send failed")).when(senderServiceMap.get(ChannelType.EMAIL)).send(notification);

        underTest.sendNotificationToDeliverySystem(dtos);

        verify(senderServiceMap.get(ChannelType.EMAIL)).send(any());
    }

    @Test
    void processDistributionListCreatesDistributionListsForDistinctCrns() {
        NotificationDto notification1 = NotificationDto.builder()
                .message("message1")
                .resourceCrn("crn1")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name1")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationDto notification2 = NotificationDto.builder()
                .message("message2")
                .resourceCrn("crn2")
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .name("name2")
                .channelType(ChannelType.EMAIL)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(notification1, notification2));

        CreateDistributionListRequest request1 = getDistributionListRequest("crn1");
        request1.setResourceName("name1");
        List<EventChannelPreference> eventChannelPreferences = List.of(new EventChannelPreference(
                "b1417842-1eef-4d65-ac36-02a0e32d424e",
                Set.of(ChannelType.EMAIL),
                Set.of(NotificationType.AZURE_DEFAULT_OUTBOUND.getNotificationSeverity())
        ));
        request1.setEventChannelPreferences(eventChannelPreferences);
        CreateDistributionListRequest request2 = getDistributionListRequest("crn2");
        request2.setResourceName("name2");
        request2.setEventChannelPreferences(eventChannelPreferences);

        Set<CreateDistributionListRequest> expectedResourceCrns = Set.of(request1, request2);
        when(converter.convert(dtos)).thenReturn(expectedResourceCrns);

        underTest.processDistributionList(dtos);

        verify(distributionListManagementService).createOrUpdateLists(expectedResourceCrns);
    }

    private CreateDistributionListRequest getDistributionListRequest(String resourceCrn) {
        CreateDistributionListRequest request = new CreateDistributionListRequest();
        request.setResourceCrn(resourceCrn);
        return request;
    }

}