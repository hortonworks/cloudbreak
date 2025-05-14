package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareIpaServerFailed;

public class PrepareIpaServerFailedToPrepareCrossRealmTrustFailureEvent implements PayloadConverter<PrepareCrossRealmTrustFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return PrepareIpaServerFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareCrossRealmTrustFailureEvent convert(Object payload) {
        PrepareIpaServerFailed result = (PrepareIpaServerFailed) payload;
        return new PrepareCrossRealmTrustFailureEvent(result.getResourceId(), result.getException());
    }
}
