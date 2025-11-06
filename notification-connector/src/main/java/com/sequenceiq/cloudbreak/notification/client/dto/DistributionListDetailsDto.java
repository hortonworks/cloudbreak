package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.List;
import java.util.Set;

public record DistributionListDetailsDto(
        String distributionListId,
        String resourceCrn,
        String resourceName,
        String parentResourceCrn,
        Set<String> slackChannelIds,
        String distributionListManagementType,
        List<String> emailAddresses
) {
}