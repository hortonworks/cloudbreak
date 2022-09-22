package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.networkfirewall.model.FirewallStatus;
import software.amazon.awssdk.services.networkfirewall.model.SyncState;

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
        return new ArrayList<>(firewallStatus.syncStates().values());
    }
}
