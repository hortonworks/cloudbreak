package com.sequenceiq.cloudbreak.common.metrics.status;

import com.sequenceiq.common.api.type.Tunnel;

public record StackCountByStatusAndTunnel(StackCountByStatusView stackCountByStatusView, Tunnel tunnel) {

    public String status() {
        return stackCountByStatusView.getStatus();
    }

    public int count() {
        return stackCountByStatusView.getCount();
    }
}
