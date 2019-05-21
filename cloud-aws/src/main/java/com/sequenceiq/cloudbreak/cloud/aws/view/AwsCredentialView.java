package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.GOV_CLOUD;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AwsCredentialView {

    private final CloudCredential cloudCredential;

    public AwsCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getRoleArn() {
        String roleArn = cloudCredential.getStringParameter("roleArn");
        if (roleArn != null) {
            return roleArn;
        }
        Map<String, String> roleBased = getRoleBased();
        if (roleBased == null) {
            return null;
        }
        return roleBased.get("roleArn");
    }

    public String getExternalId() {
        String externalId = cloudCredential.getStringParameter("externalId");
        if (externalId != null) {
            return externalId;
        }
        Map<String, String> roleBased = getRoleBased();
        if (roleBased == null) {
            return null;
        }
        return getRoleBased().get("externalId");
    }

    public Boolean isGovernmentCloudEnabled() {
        Object ev = cloudCredential.getParameter(GOV_CLOUD, Object.class);
        if (ev instanceof Boolean) {
            return (Boolean) ev;
        } else if (ev instanceof String) {
            return Boolean.parseBoolean((String) ev);
        }
        return false;
    }

    public String getAccessKey() {
        String accessKey = cloudCredential.getStringParameter("accessKey");
        if (accessKey != null) {
            return accessKey;
        }
        Map<String, String> keyBased = getKeyBased();
        if (keyBased == null) {
            return null;
        }
        return keyBased.get("accessKey");
    }

    public String getSecretKey() {
        String secretKey = cloudCredential.getStringParameter("secretKey");
        if (secretKey != null) {
            return secretKey;
        }
        Map<String, String> keyBased = getKeyBased();
        if (keyBased == null) {
            return null;
        }
        return keyBased.get("secretKey");
    }

    public Map<String, String> getKeyBased() {
        Map aws = cloudCredential.getParameter("aws", Map.class);
        if (aws == null) {
            return null;
        }
        return (Map<String, String>) aws.get("keyBased");
    }

    public Map<String, String> getRoleBased() {
        Map aws = cloudCredential.getParameter("aws", Map.class);
        if (aws == null) {
            return null;
        }
        return (Map<String, String>) aws.get("roleBased");
    }

    public Long getId() {
        return cloudCredential.getId();
    }

}
