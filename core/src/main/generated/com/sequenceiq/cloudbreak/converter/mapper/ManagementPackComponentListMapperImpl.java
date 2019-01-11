package com.sequenceiq.cloudbreak.converter.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ManagementPackComponentListMapperImpl implements ManagementPackComponentListMapper {

    @Override
    public List<ManagementPackV4Entry> mapManagementPackComponentMap(List<ManagementPackComponent> mpacks) {
        if ( mpacks == null ) {
            return null;
        }

        List<ManagementPackV4Entry> list = new ArrayList<ManagementPackV4Entry>( mpacks.size() );
        for ( ManagementPackComponent managementPackComponent : mpacks ) {
            list.add( managementPackComponentToManagementPackEntry( managementPackComponent ) );
        }

        return list;
    }

    protected ManagementPackV4Entry managementPackComponentToManagementPackEntry(ManagementPackComponent managementPackComponent) {
        if ( managementPackComponent == null ) {
            return null;
        }

        ManagementPackV4Entry managementPackV4Entry = new ManagementPackV4Entry();

        managementPackV4Entry.setMpackUrl( managementPackComponent.getMpackUrl() );

        return managementPackV4Entry;
    }
}
