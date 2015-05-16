package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;

@Component
public class AmbariStackDetailsToJsonConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetails, AmbariStackDetailsJson> {
    @Override
    public AmbariStackDetailsJson convert(AmbariStackDetails source) {
        AmbariStackDetailsJson json = new AmbariStackDetailsJson();
        json.setStack(source.getStack());
        json.setVersion(source.getVersion());
        json.setOs(source.getOs());
        json.setUtilsRepoId(source.getUtilsRepoId());
        json.setUtilsBaseURL(source.getUtilsBaseURL());
        json.setStackRepoId(source.getStackRepoId());
        json.setStackBaseURL(source.getStackBaseURL());
        json.setVerify(source.isVerify());
        return json;
    }
}
