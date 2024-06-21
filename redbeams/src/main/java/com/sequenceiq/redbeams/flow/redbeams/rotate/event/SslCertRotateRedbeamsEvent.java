package com.sequenceiq.redbeams.flow.redbeams.rotate.event;

import java.util.Objects;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class SslCertRotateRedbeamsEvent extends RedbeamsEvent implements IdempotentEvent<RedbeamsEvent> {

    private final boolean onlyCertificateUpdate;

    public SslCertRotateRedbeamsEvent(Long resourceId, boolean onlyCertificateUpdate) {
        this(null, resourceId, new Promise<>(), false, onlyCertificateUpdate);
    }

    public SslCertRotateRedbeamsEvent(Long resourceId, boolean forced, boolean onlyCertificateUpdate) {
        this(null, resourceId, new Promise<>(), forced, onlyCertificateUpdate);
    }

    public SslCertRotateRedbeamsEvent(String selector, Long resourceId, boolean onlyCertificateUpdate) {
        this(selector, resourceId, new Promise<>(), false, onlyCertificateUpdate);
    }

    public SslCertRotateRedbeamsEvent(String selector, Long resourceId, boolean forced, boolean onlyCertificateUpdate) {
        this(selector, resourceId, new Promise<>(), forced, onlyCertificateUpdate);
    }

    @JsonCreator
    public SslCertRotateRedbeamsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("forced") boolean forced,
            @JsonProperty("onlyCertificateUpdate") boolean onlyCertificateUpdate) {
        super(selector, resourceId, accepted, forced);
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    public boolean isOnlyCertificateUpdate() {
        return onlyCertificateUpdate;
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsEvent.class, other);
    }

    protected <T extends SslCertRotateRedbeamsEvent> boolean isClassAndEqualsEvent(Class<T> clazz, SslCertRotateRedbeamsEvent other) {
        return isClassAndEqualsEvent(clazz, other, redbeamsEvent -> true);
    }

    protected <T extends RedbeamsEvent> boolean isClassAndEqualsEvent(Class<T> clazz, SslCertRotateRedbeamsEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(resourceId, other.resourceId)
                && equalsSubclass.test((T) other);
    }
}
