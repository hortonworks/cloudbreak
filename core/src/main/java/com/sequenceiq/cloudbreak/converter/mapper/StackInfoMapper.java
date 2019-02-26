package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;

@Mapper(componentModel = "spring", uses = ManagementPackComponentListMapMapper.class)
public interface StackInfoMapper {

    @Mappings(@Mapping(target = "ambari", ignore = true))
    StackDescriptor mapStackInfoToStackDescriptor(StackInfo stackInfo, Map<String, List<ManagementPackComponent>> mpacks);
}
