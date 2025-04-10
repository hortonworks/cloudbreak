package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.model.SeLinux;

public class CoreModifySeLinuxEvent extends StackEvent {

    private final SeLinux selinuxMode;

    @JsonCreator
    public CoreModifySeLinuxEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("selinuxMode") SeLinux selinuxMode) {
        super(selector, resourceId);
        this.selinuxMode = selinuxMode;
    }

    public SeLinux getSelinuxMode() {
        return selinuxMode;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreModifySeLinuxEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("selinuxMode=" + selinuxMode)
                .toString();
    }
}
