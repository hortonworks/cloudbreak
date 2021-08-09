package com.sequenceiq.datalake.events;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;

@Component
public class SdxClusterDtoConverter {

    public SdxClusterDto sdxClusterToDto(SdxCluster sdxCluster) {
        SdxClusterDto sdxClusterDto = new SdxClusterDto();
        sdxClusterDto.setAccountId(sdxCluster.getAccountId());
        sdxClusterDto.setName(sdxCluster.getClusterName());
        sdxClusterDto.setResourceCrn(sdxCluster.getCrn());
        sdxClusterDto.setResourceName(sdxCluster.getClusterName());
        sdxClusterDto.setId(sdxCluster.getId());

        return sdxClusterDto;
    }

}
