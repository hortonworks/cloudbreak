package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.users.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class UpdateAmbariPasswordToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<UserNamePasswordJson, UpdateClusterJson> {

    @Override
    public UpdateClusterJson convert(UserNamePasswordJson source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        updateStackJson.setUserNamePasswordJson(source);
        return updateStackJson;
    }
}
