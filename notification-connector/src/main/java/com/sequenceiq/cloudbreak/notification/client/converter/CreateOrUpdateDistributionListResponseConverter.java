package com.sequenceiq.cloudbreak.notification.client.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.CreateOrUpdateDistributionListResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;

@Component
public class CreateOrUpdateDistributionListResponseConverter {

    public CreateOrUpdateDistributionListResponseDto convert(NotificationAdminProto.CreateOrUpdateDistributionListResponse proto) {
        if (proto == null) {
            return new CreateOrUpdateDistributionListResponseDto(null);
        }

        List<DistributionListDto> distributionListDetails = proto.getDistributionListDetailsList()
                .stream()
                .map(this::convertDistributionListDetails)
                .collect(Collectors.toList());

        return new CreateOrUpdateDistributionListResponseDto(distributionListDetails);
    }

    private DistributionListDto convertDistributionListDetails(NotificationAdminProto.DistributionListDetails proto) {
        return new DistributionListDto(
                proto.getDistributionListId(),
                proto.getResourceCrn());
    }
}
