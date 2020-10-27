package com.sequenceiq.it.cloudbreak.actor;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class CloudbreakUser {

    private String accessKey;

    private String secretKey;

    private String crn;

    private String displayName;

    private boolean admin;

    public CloudbreakUser() {
    }

    public CloudbreakUser(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        displayName = "Default User";
    }

    public CloudbreakUser(String accessKey, String secretKey, String displayName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
    }

    public CloudbreakUser(String accessKey, String secretKey, String displayName, boolean admin) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
        this.admin = admin;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCrn() {
        return crn;
    }

    public boolean getAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public static void validateRealUmsUser(CloudbreakUser user) {
        if (!Crn.isCrn(user.getCrn())) {
            throw new RuntimeException(String.format("Invalid RealUms user %s. Please check api-credentials.json", user));
        }
    }

    @Override
    public String toString() {
        return "CloudbreakUser{" +
                "crn='" + crn + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
