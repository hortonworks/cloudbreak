package com.sequenceiq.cloudbreak.job.stackpatcher.config;

public class StackPatchTypeConfig {

    private boolean enabled;

    private String entitlement;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEntitlement() {
        return entitlement;
    }

    public void setEntitlement(String entitlement) {
        this.entitlement = entitlement;
    }
}
