package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ManagementPackEntry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Mapper(componentModel = "spring")
public interface ManagementPackComponentListMapper {
    List<ManagementPackEntry> mapManagementPackComponentMap(List<ManagementPackComponent> mpacks);
}
