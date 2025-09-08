package com.sequenceiq.freeipa.service.freeipa.trust.operation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskResult(
        @JsonProperty("type") TaskResultType type,
        @JsonProperty("message") String message,
        @JsonProperty("additionalParams") Map<String, String> additionalParams) {
    @JsonCreator
    public TaskResult {
    }
}
