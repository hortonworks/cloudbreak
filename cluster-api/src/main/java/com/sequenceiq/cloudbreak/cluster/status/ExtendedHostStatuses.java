package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;

public class ExtendedHostStatuses {

    private final Map<HostName, ClusterManagerState> hostHealth;

    private final boolean hostCertExpiring;

    public ExtendedHostStatuses(Map<HostName, ClusterManagerState> hostHealth, boolean hostCertExpiring) {
        this.hostHealth = hostHealth;
        this.hostCertExpiring = hostCertExpiring;
    }

    public Map<HostName, ClusterManagerState> getHostHealth() {
        return hostHealth;
    }

    public boolean isHostCertExpiring() {
        return hostCertExpiring;
    }

    @Override
    public String toString() {
        return "ExtendedHostStatuses{" +
                "hostHealth=" + hostHealth +
                ", hostCertExpiring=" + hostCertExpiring +
                '}';
    }
}
