package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class CloudbreakNode implements Comparable<CloudbreakNode> {

    @Id
    private String uuid;

    @Column(nullable = false)
    private long lastUpdated;

    @Version
    private Long version;

    public CloudbreakNode() {
    }

    public CloudbreakNode(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudbreakNode that = (CloudbreakNode) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public int compareTo(CloudbreakNode o) {
        return o.uuid.compareToIgnoreCase(this.uuid);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakNode{");
        sb.append("uuid='").append(uuid).append('\'');
        sb.append(", lastUpdated=").append(lastUpdated);
        sb.append('}');
        return sb.toString();
    }
}
