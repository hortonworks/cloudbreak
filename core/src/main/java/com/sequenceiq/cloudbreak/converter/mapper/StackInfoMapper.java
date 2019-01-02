package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;

@Mapper(componentModel = "spring", uses = ManagementPackComponentListMapMapper.class)
public interface StackInfoMapper {

    @Mappings({
            @Mapping(target = "ambari", ignore = true)
    })
    StackDescriptorV4 mapStackInfoToStackDescriptor(StackInfo stackInfo, Map<String, List<ManagementPackComponent>> mpacks);
}
