package com.sequenceiq.cloudbreak.converter.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.sequenceiq.cloudbreak.api.model.StackDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;

@Mapper(componentModel = "spring")
public interface StackInfoMapper {

    @Mappings({
            @Mapping(target = "ambari", ignore = true)
    })
    StackDescriptor mapStackInfoToStackDescriptor(StackInfo stackInfo);

}
