package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RollingVerticalScaleDecommissionInstancesRequest extends StackEvent {

    private final String hostGroupName;

    private final Set<String> hostsNameToDecommission;

    private final RollingVerticalScaleResult rollingVerticalScaleResult;

    @JsonCreator
    public RollingVerticalScaleDecommissionInstancesRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("hostsNameToDecommission") Set<String> hostsNameToDecommission,
            @JsonProperty("rollingVerticalScaleResult") RollingVerticalScaleResult rollingVerticalScaleResult) {
        super(EventSelectorUtil.selector(RollingVerticalScaleDecommissionInstancesRequest.class), resourceId);
        this.hostGroupName = hostGroupName;
        this.hostsNameToDecommission = hostsNameToDecommission;
        this.rollingVerticalScaleResult = rollingVerticalScaleResult;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Set<String> getHostsNameToDecommission() {
        return hostsNameToDecommission;
    }

    public RollingVerticalScaleResult getRollingVerticalScaleResult() {
        return rollingVerticalScaleResult;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleDecommissionInstancesRequest.class.getSimpleName() + "[", "]")
                .add("hostGroupName=" + hostGroupName)
                .add("hostsNameToDecommission=" + hostsNameToDecommission)
                .add("rollingVerticalScaleResult=" + rollingVerticalScaleResult)
                .add(super.toString())
                .toString();
    }
}
