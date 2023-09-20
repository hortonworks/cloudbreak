package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.util.Locale;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class ExternalDatabaseSelectableEvent extends BaseNamedFlowEvent {

    @JsonCreator
    public ExternalDatabaseSelectableEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    @Override
    public String selector() {
        return StringUtils.isEmpty(super.selector()) ? selector(getClass()) : super.selector();
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT);
    }

    public static String failureSelector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT) + "_ERROR";
    }
}
