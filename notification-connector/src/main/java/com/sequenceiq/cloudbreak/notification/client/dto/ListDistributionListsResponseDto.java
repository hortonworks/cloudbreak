package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.List;

public record ListDistributionListsResponseDto(List<DistributionListDetailsDto> distributionLists) {
}
