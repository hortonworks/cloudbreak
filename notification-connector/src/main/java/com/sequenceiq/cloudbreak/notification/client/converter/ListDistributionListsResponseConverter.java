package com.sequenceiq.cloudbreak.notification.client.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDetailsDto;
import com.sequenceiq.cloudbreak.notification.client.dto.ListDistributionListsResponseDto;

@Component
public class ListDistributionListsResponseConverter {

    public ListDistributionListsResponseDto convert(NotificationAdminProto.ListDistributionListsResponse proto) {
        if (proto == null) {
            return new ListDistributionListsResponseDto(null);
        } else {
            List<DistributionListDetailsDto> distributionLists = proto.getDistributionListsList()
                    .stream()
                    .map(this::convertDistributionList)
                    .collect(Collectors.toList());

            return new ListDistributionListsResponseDto(distributionLists);
        }
    }

    private DistributionListDetailsDto convertDistributionList(NotificationAdminProto.DistributionList proto) {
        return new DistributionListDetailsDto(
                proto.getDistributionListId(),
                proto.getResourceCrn(),
                proto.getResourceName(),
                proto.getParentResourceCrn(),
                Set.copyOf(proto.getSlackChannelIdsList()),
                proto.getDistributionListManagementType().name(),
                proto.getEmailAddressesList()
        );
    }
}
