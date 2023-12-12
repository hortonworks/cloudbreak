package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import com.sequenceiq.common.api.type.Tunnel;

public record StackCountByStatusAndTunnel(StackCountByStatusView stackCountByStatusView, Tunnel tunnel) {

    public String status() {
        return stackCountByStatusView.getStatus();
    }

    public int count() {
        return stackCountByStatusView.getCount();
    }
}
