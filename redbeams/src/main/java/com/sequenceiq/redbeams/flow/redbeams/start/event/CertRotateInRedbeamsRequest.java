package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class CertRotateInRedbeamsRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    @JsonCreator
    public CertRotateInRedbeamsRequest(@JsonProperty("cloudContext") CloudContext cloudContext) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }
}
