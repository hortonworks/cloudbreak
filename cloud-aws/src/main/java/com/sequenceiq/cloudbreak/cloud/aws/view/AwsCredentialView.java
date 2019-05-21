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
        Map<String, String> roleBased = getRoleBased();
        if (roleBased == null) {
            return null;
        }
        return roleBased.get("roleArn");
    }

    public String getExternalId() {
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
        Map<String, String> keyBased = getKeyBased();
        if (keyBased == null) {
            return null;
        }
        return keyBased.get("accessKey");
    }

    public String getSecretKey() {
        return getKeyBased().get("secretKey");
    }

    public Map<String, String> getKeyBased() {
        return (Map<String, String>) cloudCredential.getParameter("aws", Map.class).get("keyBased");
    }

    public Map<String, String> getRoleBased() {
        return (Map<String, String>) cloudCredential.getParameter("aws", Map.class).get("roleBased");
    }

    public Long getId() {
        return cloudCredential.getId();
    }

}
