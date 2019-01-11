package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Mapper(componentModel = "spring")
public interface ManagementPackComponentListMapper {
    List<ManagementPackV4Entry> mapManagementPackComponentMap(List<ManagementPackComponent> mpacks);
}
