package com.sequenceiq.cloudbreak.template.views;

public class SharedServiceConfigsView {

    @Deprecated
    private String rangerAdminPassword;

    @Deprecated
    private String rangerAdminPort;

    private String rangerFqdn;

    @Deprecated
    private boolean attachedCluster;

    private boolean datalakeCluster;

    @Deprecated
    private String datalakeClusterManagerIp;

    @Deprecated
    private String datalakeClusterManagerFqdn;

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public String getRangerAdminPassword() {
        return rangerAdminPassword;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public void setRangerAdminPassword(String rangerAdminPassword) {
        this.rangerAdminPassword = rangerAdminPassword;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public boolean isAttachedCluster() {
        return attachedCluster;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public void setAttachedCluster(boolean attachedCluster) {
        this.attachedCluster = attachedCluster;
    }

    public boolean isDatalakeCluster() {
        return datalakeCluster;
    }

    public void setDatalakeCluster(boolean datalakeCluster) {
        this.datalakeCluster = datalakeCluster;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public String getRangerAdminPort() {
        return rangerAdminPort;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public void setRangerAdminPort(String rangerAdminPort) {
        this.rangerAdminPort = rangerAdminPort;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public String getDatalakeClusterManagerIp() {
        return datalakeClusterManagerIp;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public void setDatalakeClusterManagerIp(String datalakeClusterManagerIp) {
        this.datalakeClusterManagerIp = datalakeClusterManagerIp;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public String getDatalakeClusterManagerFqdn() {
        return datalakeClusterManagerFqdn;
    }

    /**
     * @deprecated it is possible that recipes are still using it, so cannot be removed
     */
    @Deprecated
    public void setDatalakeClusterManagerFqdn(String datalakeClusterManagerFqdn) {
        this.datalakeClusterManagerFqdn = datalakeClusterManagerFqdn;
    }

    public String getRangerFqdn() {
        return rangerFqdn;
    }

    public void setRangerFqdn(String rangerFqdn) {
        this.rangerFqdn = rangerFqdn;
    }
}
