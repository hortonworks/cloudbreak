package com.sequenceiq.cloudbreak.health;

public class DataSourcePoolMetadata {

    private final Integer active;

    private final Integer idle;

    private final Integer max;

    public DataSourcePoolMetadata(Integer active, Integer idle, Integer max) {
        this.active = active;
        this.idle = idle;
        this.max = max;
    }

    public Integer getActive() {
        return active;
    }

    public Integer getIdle() {
        return idle;
    }

    public Integer getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "DataSourcePoolMetadata{" +
                "active=" + active +
                ", idle=" + idle +
                ", max=" + max +
                '}';
    }
}
