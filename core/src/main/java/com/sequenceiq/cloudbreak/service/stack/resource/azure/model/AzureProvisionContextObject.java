package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class AzureProvisionContextObject extends ProvisionContextObject {

    private String commonName;
    private String osImageName;
    private volatile Set<String> allocatedIPs;

    public AzureProvisionContextObject(long stackId, String commonName, String osImageName) {
        this(stackId, commonName, osImageName, new HashSet<String>());
    }

    public AzureProvisionContextObject(long stackId, String commonName, String osImageName, Set<String> ips) {
        super(stackId);
        this.commonName = commonName;
        this.osImageName = osImageName;
        this.allocatedIPs = new HashSet<>(ips);
    }

    public String getOsImageName() {
        return osImageName;
    }

    public String getCommonName() {
        return commonName;
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
