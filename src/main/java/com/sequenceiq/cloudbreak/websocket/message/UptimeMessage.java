package com.sequenceiq.cloudbreak.websocket.message;

public class UptimeMessage {

    private Long stackId;
    private Long uptime;

    public UptimeMessage(Long stackId, Long uptime) {
        this.stackId = stackId;
        this.uptime = uptime;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Long getUptime() {
        return uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    @Override
    public String toString() {
        return "UptimeMessage [stackId=" + stackId + ", uptime=" + uptime + "]";
    }

}
