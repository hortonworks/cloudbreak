package com.sequenceiq.flow.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public abstract class AbstractSelectable implements Selectable {

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    @Override
    public String selector() {
        return selector(getClass());
    }
}
