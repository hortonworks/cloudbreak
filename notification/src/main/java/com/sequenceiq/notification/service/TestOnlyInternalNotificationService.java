package com.sequenceiq.notification.service;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.domain.test.TestOnlyInternalRegisterAzureOutboundNotificationRequest;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.scheduled.register.dto.AzureOutboundNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.sender.DistributionListManagementService;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;

@Service
public class TestOnlyInternalNotificationService {

    @Inject
    private NotificationSendingService notificationSendingService;

    @Inject
    private NotificationDataAccessService notificationService;

    @Inject
    private DistributionListManagementService distributionListManagementService;

    public void testSendWeeklyNotification(TestOnlyInternalRegisterAzureOutboundNotificationRequest testOnly) {
        notificationSendingService.processAndSend(notificationService.collectWeeklyEmailTargets());
    }

    public void testRegisterAzureDefaultOutbound(TestOnlyInternalRegisterAzureOutboundNotificationRequest request) {
        List<BaseNotificationRegisterAdditionalDataDto> list = request.getDatahubs()
                .stream()
                .map(datahub -> AzureOutboundNotificationAdditionalDataDto.builder()
                        .name(datahub.getDatahubName())
                        .crn(datahub.getDatahubCrn())
                        .creator(datahub.getCreatorName())
                        .status(datahub.getStatus())
                        .build()
                )
                .map(dto -> (BaseNotificationRegisterAdditionalDataDto) dto)
                .toList();


        notificationSendingService.registerAllNotification(
                NotificationGeneratorDtos.builder()
                        .notificationType(NotificationType.valueOf(request.getNotificationType()))
                        .notification(List.of(
                                NotificationGeneratorDto.builder()
                                        .accountId(request.getAccountId())
                                        .name(request.getEnvironmentName())
                                        .resourceCrn(request.getEnvironmentCrn())
                                        .additionalData(
                                                BaseNotificationRegisterAdditionalDataDtos.builder()
                                                        .results(list)
                                                        .build()
                                        )
                                        .build()))
                        .build());
    }

    public void testCreateOrUpdateDistributionLists(String resourceCrn) {
        CreateDistributionListRequest request = new CreateDistributionListRequest();
        request.setResourceCrn(resourceCrn);
        request.setResourceName(Crn.fromString(resourceCrn).getResource());
        request.setEventChannelPreferences(NotificationType.getEventTypeIds().stream()
                .map(id -> new EventChannelPreference(id, Set.of(ChannelType.EMAIL), Set.of(NotificationSeverity.WARNING)))
                .toList());
        distributionListManagementService.createOrUpdateLists(Set.of(request));
    }

    public void testDeleteDistributionLists(String resourceCrn) {
        distributionListManagementService.deleteDistributionList(resourceCrn);
    }

    public List<DistributionList> testListDistributionLists(String resourceCrn) {
        return distributionListManagementService.listDistributionListsForResource(resourceCrn);
    }
}
