package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ManagementPackComponentListMapMapperImpl implements ManagementPackComponentListMapMapper {

    @Autowired
    private ManagementPackComponentListMapper managementPackComponentListMapper;

    @Override
    public Map<String, List<ManagementPackV4Entry>> mapManagementPackComponentMap(Map<String, List<ManagementPackComponent>> mpacks) {
        if ( mpacks == null ) {
            return null;
        }

        Map<String, List<ManagementPackV4Entry>> map = new HashMap<String, List<ManagementPackV4Entry>>( Math.max( (int) ( mpacks.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, List<ManagementPackComponent>> entry : mpacks.entrySet() ) {
            String key = entry.getKey();
            List<ManagementPackV4Entry> value = managementPackComponentListMapper.mapManagementPackComponentMap( entry.getValue() );
            map.put( key, value );
        }

        return map;
    }
}
