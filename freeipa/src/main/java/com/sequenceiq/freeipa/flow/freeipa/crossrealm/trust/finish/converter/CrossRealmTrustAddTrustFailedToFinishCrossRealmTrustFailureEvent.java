package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustFailureEvent;

public class CrossRealmTrustAddTrustFailedToFinishCrossRealmTrustFailureEvent implements PayloadConverter<FinishCrossRealmTrustFailureEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return FinishCrossRealmTrustAddTrustFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public FinishCrossRealmTrustFailureEvent convert(Object payload) {
        FinishCrossRealmTrustAddTrustFailed result = (FinishCrossRealmTrustAddTrustFailed) payload;
        return new FinishCrossRealmTrustFailureEvent(result.getResourceId(), result.getException());
    }
}
