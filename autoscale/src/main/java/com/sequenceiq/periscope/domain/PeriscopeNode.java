package com.sequenceiq.periscope.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class PeriscopeNode implements Comparable<PeriscopeNode> {

    @Id
    private String uuid;

    @Column(nullable = false)
    private long lastUpdated;

    @Column(nullable = false)
    private boolean leader;

    @Version
    private Long version;

    public PeriscopeNode() {
    }

    public PeriscopeNode(String uuid) {
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

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
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
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }

        PeriscopeNode that = (PeriscopeNode) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public int compareTo(PeriscopeNode o) {
        return o.uuid.compareToIgnoreCase(uuid);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PeriscopeNode{");
        sb.append("uuid='").append(uuid).append('\'');
        sb.append("leader='").append(leader).append('\'');
        sb.append(", lastUpdated=").append(lastUpdated);
        sb.append('}');
        return sb.toString();
    }
}
