package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class AzureProvisionContextObject extends ProvisionContextObject {

    private String affinityGroupName;
    private volatile Set<String> allocatedIPs;

    public AzureProvisionContextObject(long stackId, String affinityGroupName) {
        this(stackId, affinityGroupName, new HashSet<String>());
    }

    public AzureProvisionContextObject(long stackId, String affinityGroupName, Set<String> ips) {
        super(stackId);
        this.affinityGroupName = affinityGroupName;
        this.allocatedIPs = new HashSet<>(ips);
    }

    public String getAffinityGroupName() {
        return affinityGroupName;
    }

    public synchronized boolean putIfAbsent(String ip) {
        boolean added = false;
        if (!allocatedIPs.contains(ip)) {
            allocatedIPs.add(ip);
            added = true;
        }
        return added;
    }
}
