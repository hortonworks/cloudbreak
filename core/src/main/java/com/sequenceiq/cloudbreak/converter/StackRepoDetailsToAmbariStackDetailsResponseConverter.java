package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import org.springframework.stereotype.Component;

@Component
public class StackRepoDetailsToAmbariStackDetailsResponseConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, AmbariStackDetailsResponse> {

    @Override
    public AmbariStackDetailsResponse convert(StackRepoDetails source) {
        AmbariStackDetailsResponse ambariRepoDetailsJson = new AmbariStackDetailsResponse();
        ambariRepoDetailsJson.setHdpVersion(source.getHdpVersion());
        ambariRepoDetailsJson.setVerify(source.isVerify());
        ambariRepoDetailsJson.setKnox(source.getKnox());
        ambariRepoDetailsJson.setStack(source.getStack());
        ambariRepoDetailsJson.setUtil(source.getUtil());
        ambariRepoDetailsJson.setEnableGplRepo(source.isEnableGplRepo());
        return ambariRepoDetailsJson;
    }
}
