package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class AmbariRepoDetailsMapperImpl implements AmbariRepoDetailsMapper {

    @Override
    public AmbariRepoDetailsJson mapAmbariRepoDetailsToAmbariRepoDetailsJson(AmbariRepoDetails ambariRepoDetails) {
        if ( ambariRepoDetails == null ) {
            return null;
        }

        AmbariRepoDetailsJson ambariRepoDetailsJson = new AmbariRepoDetailsJson();

        ambariRepoDetailsJson.setBaseUrl( ambariRepoDetails.getBaseurl() );
        ambariRepoDetailsJson.setGpgKeyUrl( ambariRepoDetails.getGpgkey() );

        ambariRepoDetailsJson.setVersion( ambariRepoDetails.getBaseurl().split("/")[ambariRepoDetails.getBaseurl().split("/").length - 1] );

        return ambariRepoDetailsJson;
    }
}
