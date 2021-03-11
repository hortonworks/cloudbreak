package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.networkfirewall.model.FirewallStatus;
import com.amazonaws.services.networkfirewall.model.SyncState;

public class AwsFirewall {

    private final String name;

    private final FirewallStatus firewallStatus;

    public AwsFirewall(String name, FirewallStatus firewallStatus) {
        this.name = name;
        this.firewallStatus = firewallStatus;
    }

    public String getName() {
        return name;
    }

    public FirewallStatus getFirewallStatus() {
        return firewallStatus;
    }

    public List<SyncState> getAllSyncStates() {
        return new ArrayList<>(firewallStatus.getSyncStates().values());
    }
}
