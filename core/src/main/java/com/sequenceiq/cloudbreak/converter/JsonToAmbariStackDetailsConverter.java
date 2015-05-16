package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;

@Component
public class JsonToAmbariStackDetailsConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, AmbariStackDetails> {
    @Override
    public AmbariStackDetails convert(AmbariStackDetailsJson source) {
        AmbariStackDetails stackDetails = new AmbariStackDetails();
        stackDetails.setStack(source.getStack());
        stackDetails.setVersion(source.getVersion());
        stackDetails.setOs(source.getOs());
        stackDetails.setUtilsRepoId(source.getUtilsRepoId());
        stackDetails.setUtilsBaseURL(source.getUtilsBaseURL());
        stackDetails.setStackRepoId(source.getStackRepoId());
        stackDetails.setStackBaseURL(source.getStackBaseURL());
        stackDetails.setVerify(source.getVerify());
        return stackDetails;
    }
}
