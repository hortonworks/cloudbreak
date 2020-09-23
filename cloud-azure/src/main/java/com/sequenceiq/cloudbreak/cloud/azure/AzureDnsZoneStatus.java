package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.StringJoiner;

import com.microsoft.azure.management.privatedns.v2018_09_01.PrivateZone;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;

public class AzureDnsZoneStatus {

    private PrivateZone dnsZone;

    private List<VirtualNetworkLinkInner> networkLinkList;

    private AzurePrivateDnsZoneServiceEnum service;

    public PrivateZone getDnsZone() {
        return dnsZone;
    }

    public void setDnsZone(PrivateZone dnsZone) {
        this.dnsZone = dnsZone;
    }

    public List<VirtualNetworkLinkInner> getNetworkLinkList() {
        return networkLinkList;
    }

    public void setNetworkLinkList(List<VirtualNetworkLinkInner> networkLinkList) {
        this.networkLinkList = networkLinkList;
    }

    public AzurePrivateDnsZoneServiceEnum getService() {
        return service;
    }

    public void setService(AzurePrivateDnsZoneServiceEnum service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureDnsZoneStatus.class.getSimpleName() + "[", "]")
                .add("dnsZone=" + dnsZone)
                .add("networkLinkList=" + networkLinkList)
                .add("service=" + service)
                .toString();
    }
}
