package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.List;

public record CreateOrUpdateDistributionListResponseDto(List<DistributionListDto> distributionLists) {
}