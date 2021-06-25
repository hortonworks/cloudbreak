package com.sequenceiq.datalake.flow;

import com.sequenceiq.datalake.entity.SdxCluster;
import org.springframework.stereotype.Component;

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
