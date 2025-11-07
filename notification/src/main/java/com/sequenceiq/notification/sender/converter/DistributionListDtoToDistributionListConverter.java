package com.sequenceiq.notification.sender.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;

@Component
public class DistributionListDtoToDistributionListConverter {

    public DistributionList convert(DistributionListDto distributionListDto) {
        if (distributionListDto == null) {
            return null;
        }

        return DistributionList.builder()
                .resourceCrn(distributionListDto.resourceCrn())
                .externalDistributionListId(distributionListDto.distributionListId())
                .type(DistributionListManagementType.USER_MANAGED)
                .build();
    }

    public DistributionList convert(DistributionListDto distributionListDto, DistributionListManagementType type) {
        if (distributionListDto == null) {
            return null;
        }

        return DistributionList.builder()
                .resourceCrn(distributionListDto.resourceCrn())
                .externalDistributionListId(distributionListDto.distributionListId())
                .type(type != null ? type : DistributionListManagementType.USER_MANAGED)
                .build();
    }
}
