package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonValue;

public enum RecordStatus implements Serializable {
    SENT, PENDING, UNKNOWN;

    @JsonValue
    final String value() {
        return name();
    }
}
