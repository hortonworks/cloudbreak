package com.sequenceiq.cloudbreak.common.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = CLASS, property = "@type")
public interface Payload {

    Long getResourceId();

    default Exception getException() {
        return null;
    }
}
