package com.sequenceiq.flow.core.helloworld;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class HelloWorldSelectableEvent implements Selectable {
    private Long resourceId;

    private String selector;

    public HelloWorldSelectableEvent(Long resourceId) {
        this(resourceId, null);
    }

    public HelloWorldSelectableEvent(Long resourceId, String selector) {
        this.resourceId = resourceId;
        this.selector = selector;
    }

    @Override
    public String selector() {
        return StringUtils.isEmpty(selector) ? selector(getClass()) : selector;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public static String failureSelector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase() + "_ERROR";
    }
}
