package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class AzureProvisionContextObject extends ProvisionContextObject {

    private String affinityGroupName;
    private boolean useGlobalStorageAccount;
    private volatile Set<String> allocatedIPs;
    private volatile int vhdPerStorage[];

    public AzureProvisionContextObject(long stackId, String affinityGroupName, Set<String> ips, boolean useGlobalStorageAccount) {
        this(stackId, affinityGroupName, ips, new int[0], useGlobalStorageAccount);
    }

    public AzureProvisionContextObject(long stackId, String affinityGroupName, Set<String> ips, int vhdPerStorage[]) {
        this(stackId, affinityGroupName, ips, vhdPerStorage, false);
    }

    private AzureProvisionContextObject(long stackId, String affinityGroupName, Set<String> ips, int vhdPerStorage[], boolean useGlobalStorageAccount) {
        super(stackId);
        this.affinityGroupName = affinityGroupName;
        this.allocatedIPs = new HashSet<>(ips);
        this.vhdPerStorage = Arrays.copyOf(vhdPerStorage, vhdPerStorage.length);
        this.useGlobalStorageAccount = useGlobalStorageAccount;
    }

    public String getAffinityGroupName() {
        return affinityGroupName;
    }

    public boolean isUseGlobalStorageAccount() {
        return useGlobalStorageAccount;
    }

    public synchronized boolean putIfAbsent(String ip) {
        boolean added = false;
        if (!allocatedIPs.contains(ip)) {
            allocatedIPs.add(ip);
            added = true;
        }
        return added;
    }

    public synchronized int setAndGetStorageAccountIndex(int volumeCount) {
        for (int i = 0; i < vhdPerStorage.length; i++) {
            int space = vhdPerStorage[i];
            if (space >= volumeCount) {
                vhdPerStorage[i] = space - volumeCount;
                return i;
            }
        }
        return -1;
    }
}
