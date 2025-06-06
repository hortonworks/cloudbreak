package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class StackFailureToPrepareCrossRealmTrustFailureEvent implements PayloadConverter<PrepareCrossRealmTrustFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return StackFailureEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareCrossRealmTrustFailureEvent convert(Object payload) {
        StackFailureEvent result = (StackFailureEvent) payload;
        return new PrepareCrossRealmTrustFailureEvent(result.getResourceId(), result.getException());
    }
}
