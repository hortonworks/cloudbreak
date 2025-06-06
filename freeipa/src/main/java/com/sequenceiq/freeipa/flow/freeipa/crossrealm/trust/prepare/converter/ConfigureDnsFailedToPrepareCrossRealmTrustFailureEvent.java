package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareCrossRealmTrustFailureEvent;

public class ConfigureDnsFailedToPrepareCrossRealmTrustFailureEvent implements PayloadConverter<PrepareCrossRealmTrustFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ConfigureDnsFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareCrossRealmTrustFailureEvent convert(Object payload) {
        ConfigureDnsFailed result = (ConfigureDnsFailed) payload;
        return new PrepareCrossRealmTrustFailureEvent(result.getResourceId(), result.getException());
    }
}
