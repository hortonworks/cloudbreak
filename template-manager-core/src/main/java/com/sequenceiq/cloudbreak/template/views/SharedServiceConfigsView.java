package com.sequenceiq.cloudbreak.template.views;

import java.util.HashSet;
import java.util.Set;

public class SharedServiceConfigsView {

    private String rangerAdminPassword;

    private boolean attachedCluster;

    private boolean datalakeCluster;

    private String rangerAdminPort;

    private String datalakeAmbariIp;

    private String datalakeAmbariFqdn;

    private Set<String> datalakeComponents = new HashSet<>();

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

    public String getDatalakeAmbariIp() {
        return datalakeAmbariIp;
    }

    public void setDatalakeAmbariIp(String datalakeAmbariIp) {
        this.datalakeAmbariIp = datalakeAmbariIp;
    }

    public String getDatalakeAmbariFqdn() {
        return datalakeAmbariFqdn;
    }

    public void setDatalakeAmbariFqdn(String datalakeAmbariFqdn) {
        this.datalakeAmbariFqdn = datalakeAmbariFqdn;
    }

    public Set<String> getDatalakeComponents() {
        return datalakeComponents;
    }

    public void setDatalakeComponents(Set<String> datalakeComponents) {
        this.datalakeComponents = datalakeComponents;
    }
}
