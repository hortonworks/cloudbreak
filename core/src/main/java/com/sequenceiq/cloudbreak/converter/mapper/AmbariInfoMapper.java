package com.sequenceiq.cloudbreak.converter.mapper;

import org.mapstruct.Mapper;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;

@Mapper(componentModel = "spring", uses = AmbariRepoDetailsMapper.class)
public interface AmbariInfoMapper {

    AmbariInfoJson mapAmbariInfoToAmbariInfoJson(AmbariInfo ambariInfo);
}
