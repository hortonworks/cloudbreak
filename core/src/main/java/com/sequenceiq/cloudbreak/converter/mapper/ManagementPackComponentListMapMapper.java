package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Mapper(componentModel = "spring", uses = ManagementPackComponentListMapper.class)
public interface ManagementPackComponentListMapMapper {
    Map<String, List<ManagementPackV4Entry>> mapManagementPackComponentMap(Map<String, List<ManagementPackComponent>> mpacks);
}
