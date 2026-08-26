package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollingVerticalScaleCommissionInstancesRequest extends StackEvent {

    private final String hostGroupName;

    private final List<InstanceMetadataView> startedInstancesToCommission;

    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleCommissionInstancesRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("startedInstancesToCommission") List<InstanceMetadataView> startedInstancesToCommission,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(EventSelectorUtil.selector(RollingVerticalScaleCommissionInstancesRequest.class), resourceId);
        this.hostGroupName = hostGroupName;
        this.startedInstancesToCommission = startedInstancesToCommission;
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public List<InstanceMetadataView> getStartedInstancesToCommission() {
        return startedInstancesToCommission;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleCommissionInstancesRequest.class.getSimpleName() + "[", "]")
                .add("hostGroupName=" + hostGroupName)
                .add("startedInstancesToCommission=" + startedInstancesToCommission)
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
