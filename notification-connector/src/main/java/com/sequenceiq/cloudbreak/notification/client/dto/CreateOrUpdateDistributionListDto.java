package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.List;
import java.util.Set;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;

public record CreateOrUpdateDistributionListDto(
        String resourceCrn,
        String resourceName,
        List<NotificationAdminProto.EventChannelPreference> eventChannelPreferences,
        Set<String> emailAddresses,
        String distributionListId,
        String parentResourceCrn,
        Set<String> slackChannelIds,
        String distributionListManagementType) {
}
