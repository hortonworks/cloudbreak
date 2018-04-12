package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Component
public class UpdateAmbariPasswordToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<UserNamePasswordJson, UpdateClusterJson> {

    @Inject
    private BlueprintRepository blueprintRepository;

    @Override
    public UpdateClusterJson convert(UserNamePasswordJson source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        updateStackJson.setUserNamePasswordJson(source);
        return updateStackJson;
    }
}
