package com.sequenceiq.redbeams.flow.redbeams.rotate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SslCertRotateDatabaseServerSuccess extends SslCertRotateRedbeamsEvent {

    @JsonCreator
    public SslCertRotateDatabaseServerSuccess(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("onlyCertificateUpdate") boolean onlyCertificateUpdate) {
        super(resourceId, onlyCertificateUpdate);
    }
}
