package com.sequenceiq.cloudbreak.cloud.event;

public interface Selectable extends Payload {
    String selector();
}
