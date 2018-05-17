package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ManagementPackEntry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ManagementPackComponentListMapperImpl implements ManagementPackComponentListMapper {

    @Override
    public List<ManagementPackEntry> mapManagementPackComponentMap(List<ManagementPackComponent> mpacks) {
        if ( mpacks == null ) {
            return null;
        }

        List<ManagementPackEntry> list = new ArrayList<ManagementPackEntry>( mpacks.size() );
        for ( ManagementPackComponent managementPackComponent : mpacks ) {
            list.add( managementPackComponentToManagementPackEntry( managementPackComponent ) );
        }

        return list;
    }

    protected ManagementPackEntry managementPackComponentToManagementPackEntry(ManagementPackComponent managementPackComponent) {
        if ( managementPackComponent == null ) {
            return null;
        }

        ManagementPackEntry managementPackEntry = new ManagementPackEntry();

        managementPackEntry.setMpackUrl( managementPackComponent.getMpackUrl() );

        return managementPackEntry;
    }
}
