package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class AzureCredential extends Credential implements ProvisionEntity {

    private static final int END_INDEX = 24;
    private String subscriptionId;
    @Column(columnDefinition = "TEXT")
    private String cerFile;
    @Column(columnDefinition = "TEXT")
    private String jksFile;
    @Column(columnDefinition = "TEXT")
    private String sshCerFile;
    private String jks;

    private String postFix;

    public AzureCredential() {

    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public String getPostFix() {
        return postFix;
    }

    public String getCerFile() {
        return cerFile;
    }

    public void setCerFile(String cerFile) {
        this.cerFile = cerFile;
    }

    public String getJksFile() {
        return jksFile;
    }

    public void setJksFile(String jksFile) {
        this.jksFile = jksFile;
    }

    public String getSshCerFile() {
        return sshCerFile;
    }

    public void setSshCerFile(String sshCerFile) {
        this.sshCerFile = sshCerFile;
    }

    public void setPostFix(String postFix) {
        this.postFix = postFix;
    }

    public String getAffinityGroupName(CloudRegion location) {
        String result = subscriptionId.replaceAll("\\s+", "").replaceAll("-", "") + location.region().toLowerCase().replaceAll(" ", "");
        if (result.length() > END_INDEX) {
            return result.substring(result.length() - END_INDEX, result.length());
        }
        return result;
    }
}
