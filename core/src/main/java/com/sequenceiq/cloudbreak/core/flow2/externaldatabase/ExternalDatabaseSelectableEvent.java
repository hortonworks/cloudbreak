package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import org.springframework.util.StringUtils;

import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class ExternalDatabaseSelectableEvent extends BaseNamedFlowEvent {

    public ExternalDatabaseSelectableEvent(Long resourceId, String selector, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    @Override
    public String selector() {
        return StringUtils.isEmpty(super.selector()) ? selector(getClass()) : super.selector();
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public static String failureSelector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase() + "_ERROR";
    }
}
