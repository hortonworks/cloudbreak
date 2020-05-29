package com.sequenceiq.environment.environment.repository.converter;

import java.util.Optional;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors;

public class EnvDeleteStateSelectorsConverter extends DefaultEnumConverter<EnvDeleteStateSelectors> {
    @Override
    public EnvDeleteStateSelectors getDefault() {
        return EnvDeleteStateSelectors.FINALIZE_ENV_DELETE_EVENT;
    }

    @Override
    public Optional<EnvDeleteStateSelectors> tryConvertUnknownField(String attribute) {
        if ("START_PREREQUISITES_DELETE_EVENT".equals(attribute)) {
            return Optional.of(EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT);
        }
        return Optional.empty();
    }
}
