package com.sequenceiq.cloudbreak.domain.stack.cluster;

public class UptimeStat {

    private Long upSince;

    private String uptime;

    public UptimeStat() {
    }

    public UptimeStat(Long upSince, String uptime) {
        this.upSince = upSince;
        this.uptime = uptime;
    }

    public Long getUpSince() {
        return upSince;
    }

    public void setUpSince(Long upSince) {
        this.upSince = upSince;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }
}
