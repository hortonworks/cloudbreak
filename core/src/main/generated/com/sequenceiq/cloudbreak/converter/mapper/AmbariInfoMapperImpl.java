package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class AmbariInfoMapperImpl implements AmbariInfoMapper {

    @Autowired
    private AmbariRepoDetailsMapper ambariRepoDetailsMapper;

    @Override
    public AmbariInfoJson mapAmbariInfoToAmbariInfoJson(AmbariInfo ambariInfo) {
        if ( ambariInfo == null ) {
            return null;
        }

        AmbariInfoJson ambariInfoJson = new AmbariInfoJson();

        ambariInfoJson.setVersion( ambariInfo.getVersion() );
        ambariInfoJson.setRepo( stringAmbariRepoDetailsMapToStringAmbariRepoDetailsJsonMap( ambariInfo.getRepo() ) );

        return ambariInfoJson;
    }

    protected Map<String, AmbariRepoDetailsJson> stringAmbariRepoDetailsMapToStringAmbariRepoDetailsJsonMap(Map<String, AmbariRepoDetails> map) {
        if ( map == null ) {
            return null;
        }

        Map<String, AmbariRepoDetailsJson> map1 = new HashMap<String, AmbariRepoDetailsJson>( Math.max( (int) ( map.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, AmbariRepoDetails> entry : map.entrySet() ) {
            String key = entry.getKey();
            AmbariRepoDetailsJson value = ambariRepoDetailsMapper.mapAmbariRepoDetailsToAmbariRepoDetailsJson( entry.getValue() );
            map1.put( key, value );
        }

        return map1;
    }
}
