package com.sequenceiq.cloudbreak.cloud.model.dns;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CloudPrivateDnsZone {

    private String privateDnsZoneId;

    public CloudPrivateDnsZone(String privateDnsZoneId) {
        this.privateDnsZoneId = privateDnsZoneId;
    }

    public String getPrivateDnsZoneId() {
        return privateDnsZoneId;
    }

    public void setPrivateDnsZoneId(String privateDnsZoneId) {
        this.privateDnsZoneId = privateDnsZoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudPrivateDnsZone that = (CloudPrivateDnsZone) o;

        return new EqualsBuilder().append(getPrivateDnsZoneId(), that.getPrivateDnsZoneId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getPrivateDnsZoneId()).toHashCode();
    }

    @Override
    public String toString() {
        return "CloudPrivateDnsZone{" +
                "privateDnsZoneId='" + privateDnsZoneId + '\'' +
                '}';
    }

}