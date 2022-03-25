package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event;

import java.util.Optional;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.clusterproxy.Tunnel;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeCcmEvent extends StackEvent {
    private Tunnel oldTunnel;

    private CcmConnectivityParameters ccmConnectivityParameters;

    public UpgradeCcmEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public UpgradeCcmEvent(String selector, Long stackId, Tunnel oldTunnel, CcmConnectivityParameters ccmConnectivityParameters) {
        super(selector, stackId);
        this.ccmConnectivityParameters = ccmConnectivityParameters;
        this.oldTunnel = oldTunnel;
    }

    public Optional<Tunnel> getOldTunnel() {
        return Optional.ofNullable(oldTunnel);
    }

    public void setOldTunnel(Tunnel oldTunnel) {
        this.oldTunnel = oldTunnel;
    }

    public Optional<CcmConnectivityParameters> getCcmConnectivityParameters() {
        return Optional.ofNullable(ccmConnectivityParameters);
    }

    public void setCcmConnectivityParameters(CcmConnectivityParameters ccmConnectivityParameters) {
        this.ccmConnectivityParameters = ccmConnectivityParameters;
    }

    @Override
    public String toString() {
        return "UpgradeCcmEvent{" +
                ", oldTunnel=" + oldTunnel +
                "} " + super.toString();
    }
}
