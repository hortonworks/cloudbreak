package com.sequenceiq.it.cloudbreak.actor;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class CloudbreakUser {

    private String accessKey;

    private String secretKey;

    private String crn;

    private String displayName;

    private String workloadUserName;

    private boolean admin;

    private String description;

    private String workloadPassword;

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

    public CloudbreakUser(String accessKey, String secretKey, String displayName, String crn) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
        this.crn = crn;
    }

    public CloudbreakUser(String accessKey, String secretKey, String displayName, boolean admin) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
        this.admin = admin;
    }

    public CloudbreakUser(String accessKey, String secretKey, String displayName, String crn, String description, boolean admin, String workloadUserName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.displayName = displayName;
        this.crn = crn;
        this.description = description;
        this.admin = admin;
        this.workloadUserName = workloadUserName;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getCrn() {
        return crn;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean getAdmin() {
        return admin;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return description;
    }

    public void setWorkloadUserName(String workloadUserName) {
        this.workloadUserName = workloadUserName;
    }

    public String getWorkloadUserName() {
        return workloadUserName;
    }

    public String getWorkloadPassword() {
        return workloadPassword;
    }

    public void setWorkloadPassword(String workloadPassword) {
        this.workloadPassword = workloadPassword;
    }

    public static void validateRealUmsUser(CloudbreakUser user) {
        if (!Crn.isCrn(user.getCrn())) {
            throw new RuntimeException(String.format("Invalid real UMS user %s. Please check api-credentials.json", user));
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
