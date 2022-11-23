package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class CertRotateInRedbeamsSuccess extends RedbeamsEvent {

    @JsonCreator
    public CertRotateInRedbeamsSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
