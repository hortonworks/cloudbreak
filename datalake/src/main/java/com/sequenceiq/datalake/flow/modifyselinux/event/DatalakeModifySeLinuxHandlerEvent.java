package com.sequenceiq.datalake.flow.modifyselinux.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class DatalakeModifySeLinuxHandlerEvent extends BaseNamedFlowEvent {

    private final SeLinux selinuxMode;

    @JsonCreator
    public DatalakeModifySeLinuxHandlerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("selinuxMode") SeLinux selinuxMode) {
        super(EventSelectorUtil.selector(DatalakeModifySeLinuxHandlerEvent.class), resourceId, resourceName, resourceCrn);
        this.selinuxMode = selinuxMode;
    }

    public SeLinux getSelinuxMode() {
        return selinuxMode;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DatalakeModifySeLinuxHandlerEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("resourceId=" + getResourceId())
                .add("resourceName=" + getResourceName())
                .add("resourceCrn=" + getResourceCrn())
                .add("selinuxMode=" + selinuxMode)
                .toString();
    }
}
