package com.sequenceiq.periscope.monitor.event;

import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.domain.BaseAlert;

public class ScalingEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1587121489597L;

    private List<String> decommissionNodeIds;

    private transient Optional<Integer> scalingNodeCount = Optional.empty();

    private transient Optional<Integer> hostGroupNodeCount = Optional.empty();

    public ScalingEvent(BaseAlert alert) {
        super(alert);
    }

    public BaseAlert getAlert() {
        return (BaseAlert) getSource();
    }

    public List<String> getDecommissionNodeIds() {
        return decommissionNodeIds != null ? decommissionNodeIds : List.of();
    }

    public void setDecommissionNodeIds(List<String> decommissionNodeIds) {
        this.decommissionNodeIds = decommissionNodeIds;
    }

    public Optional<Integer> getScalingNodeCount() {
        return scalingNodeCount;
    }

    public void setScalingNodeCount(Optional<Integer> scalingNodeCount) {
        this.scalingNodeCount = scalingNodeCount;
    }

    public Optional<Integer> getHostGroupNodeCount() {
        return hostGroupNodeCount;
    }

    public void setHostGroupNodeCount(Optional<Integer> hostGroupNodeCount) {
        this.hostGroupNodeCount = hostGroupNodeCount;
    }
}
