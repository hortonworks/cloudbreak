package com.sequenceiq.cloudbreak.cloud.model.dns;

import java.util.ArrayList;
import java.util.List;

public class CloudPrivateDnsZones {

    private List<CloudPrivateDnsZone> privateDnsZones = new ArrayList<>();

    public CloudPrivateDnsZones() {
    }

    public CloudPrivateDnsZones(List<CloudPrivateDnsZone> privateDnsZones) {
        this.privateDnsZones = privateDnsZones;
    }

    public List<CloudPrivateDnsZone> getPrivateDnsZones() {
        return privateDnsZones;
    }

    public void setPrivateDnsZones(List<CloudPrivateDnsZone> privateDnsZones) {
        this.privateDnsZones = privateDnsZones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudPrivateDnsZones that = (CloudPrivateDnsZones) o;

        return getPrivateDnsZones() != null
                ? getPrivateDnsZones().equals(that.getPrivateDnsZones())
                : that.getPrivateDnsZones() == null;
    }

    @Override
    public int hashCode() {
        return getPrivateDnsZones() != null ? getPrivateDnsZones().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CloudPrivateDnsZones{" +
                "privateDnsZones=" + privateDnsZones +
                '}';
    }

}