package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustFailureEvent;

public class CrossRealmTrustValidationFailedToPrepareCrossRealmTrustFailureEvent implements PayloadConverter<PrepareCrossRealmTrustFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CrossRealmTrustValidationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareCrossRealmTrustFailureEvent convert(Object payload) {
        CrossRealmTrustValidationFailed result = (CrossRealmTrustValidationFailed) payload;
        return new PrepareCrossRealmTrustFailureEvent(result.getResourceId(), result.getException());
    }
}
