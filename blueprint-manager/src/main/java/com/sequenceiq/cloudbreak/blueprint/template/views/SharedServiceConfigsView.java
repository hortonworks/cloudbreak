package com.sequenceiq.cloudbreak.blueprint.template.views;

public class SharedServiceConfigsView {

    private String rangerAdminPassword;

    private boolean attachedCluster;

    private boolean datalakeCluster;

    private String rangerAdminPort;

    public String getRangerAdminPassword() {
        return rangerAdminPassword;
    }

    public void setRangerAdminPassword(String rangerAdminPassword) {
        this.rangerAdminPassword = rangerAdminPassword;
    }

    public boolean isAttachedCluster() {
        return attachedCluster;
    }

    public void setAttachedCluster(boolean attachedCluster) {
        this.attachedCluster = attachedCluster;
    }

    public boolean isDatalakeCluster() {
        return datalakeCluster;
    }

    public void setDatalakeCluster(boolean datalakeCluster) {
        this.datalakeCluster = datalakeCluster;
    }

    public String getRangerAdminPort() {
        return rangerAdminPort;
    }

    public void setRangerAdminPort(String rangerAdminPort) {
        this.rangerAdminPort = rangerAdminPort;
    }
}
