package com.sequenceiq.cloudbreak.service.stack.event;

public class AmbariRoleAllocationComplete {

    private Long stackId;
    private String ambariIp;

    public AmbariRoleAllocationComplete(Long stackId, String ambariIp) {
        this.stackId = stackId;
        this.ambariIp = ambariIp;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

}
