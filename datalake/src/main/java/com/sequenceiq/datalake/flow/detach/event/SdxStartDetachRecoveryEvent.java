package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class SdxStartDetachRecoveryEvent extends SdxEvent {
    public SdxStartDetachRecoveryEvent(String selector, Long detachedSdxId, String userId, Promise<AcceptResult> accepted) {
        super(selector, detachedSdxId, userId, accepted);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartDetachRecoveryEvent.class, other,
                event -> Objects.equals(event.getResourceId(), other.getResourceId()));
    }

    @Override
    public String toString() {
        return selector() + '{' + "detachedSdxId: '" + getResourceId() + "'}";
    }
}
