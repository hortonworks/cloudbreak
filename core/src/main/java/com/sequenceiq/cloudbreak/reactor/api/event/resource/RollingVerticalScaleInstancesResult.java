package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_START_INSTANCES_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RollingVerticalScaleInstancesResult extends StackEvent {
    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleInstancesResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(ROLLING_VERTICALSCALE_START_INSTANCES_EVENT.event(), resourceId);
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleInstancesResult.class.getSimpleName() + "[", "]")
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
