package com.sequenceiq.environment.environment.repository.converter;

import java.util.Optional;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors;

public class EnvCreationStateSelectorsConverter extends DefaultEnumConverter<EnvCreationStateSelectors> {

    @Override
    public EnvCreationStateSelectors getDefault() {
        return EnvCreationStateSelectors.FINALIZE_ENV_CREATION_EVENT;
    }

    @Override
    public Optional<EnvCreationStateSelectors> tryConvertUnknownField(String attribute) {
        if ("START_PREREQUISITES_CREATION_EVENT".equals(attribute)) {
            return Optional.of(EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT);
        }
        return Optional.empty();
    }
}
