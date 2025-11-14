package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class MinionStatusWithTimestamp extends MinionStatus {

    private Long timestamp;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MinionStatusWithTimestamp{"
                + "down=" + getDown()
                + ", up=" + getUp()
                + ", timestamp=" + timestamp
                + '}';
    }
}
