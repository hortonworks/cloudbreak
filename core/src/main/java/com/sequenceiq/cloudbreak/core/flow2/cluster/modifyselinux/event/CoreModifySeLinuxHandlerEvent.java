package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class CoreModifySeLinuxHandlerEvent extends StackEvent {

    private final SeLinux selinuxMode;

    @JsonCreator
    public CoreModifySeLinuxHandlerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selinuxMode") SeLinux selinuxMode) {
        super(EventSelectorUtil.selector(CoreModifySeLinuxHandlerEvent.class), resourceId);
        this.selinuxMode = selinuxMode;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(CoreModifySeLinuxHandlerEvent.class, other,
                event -> Objects.equals(getResourceId(), event.getResourceId()));
    }

    public SeLinux getSelinuxMode() {
        return selinuxMode;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreModifySeLinuxHandlerEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("resourceId=" + getResourceId())
                .add("selinuxMode=" + selinuxMode)
                .toString();
    }
}
