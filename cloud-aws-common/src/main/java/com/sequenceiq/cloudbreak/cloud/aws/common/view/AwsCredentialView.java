package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.GOV_CLOUD;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.aws.common.AssumeRoleType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AwsCredentialView {

    public static final String DEFAULT_REGION_KEY = "defaultRegion";

    public static final String AWS = "aws";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialView.class);

    private static final String ROLE_ARN = "roleArn";

    private static final String SECRET_KEY = "secretKey";

    private static final String ACCESS_KEY = "accessKey";

    private static final String KEY_BASED = "keyBased";

    private static final String ROLE_BASED = "roleBased";

    private static final String EXTERNAL_ID = "externalId";

    private static final String ROLE_ASSUME_TYPE = "roleAssumeType";

    private final CloudCredential cloudCredential;

    public AwsCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;
    }

    public String getName() {
        return cloudCredential.getName();
    }

    // This method is used in AwsSessionCredentialClient.java for caching
    public String getId() {
        return cloudCredential.getId();
    }

    public String getRoleArn() {
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> roleBased = getRoleBased();
            if (roleBased == null) {
                return null;
            }
            return roleBased.get(ROLE_ARN);
        }
        return cloudCredential.getParameter(ROLE_ARN, String.class);
    }

    public String getExternalId() {
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> roleBased = getRoleBased();
            if (roleBased == null) {
                return null;
            }
            return getRoleBased().get(EXTERNAL_ID);
        }
        return cloudCredential.getParameter(EXTERNAL_ID, String.class);
    }

    public AssumeRoleType getRoleAssumeType() {
        String value;
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> roleBased = getRoleBased();
            if (roleBased == null) {
                return null;
            }
            value = roleBased.get(ROLE_ASSUME_TYPE);
        } else {
            value = cloudCredential.getParameter(ROLE_ASSUME_TYPE, String.class);
        }
        return AssumeRoleType.fromString(value);
    }

    public void setRoleAssumeType(AssumeRoleType roleAssumeType) {
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> roleBased = getRoleBased();
            if (roleBased != null) {
                roleBased.put(ROLE_ASSUME_TYPE, roleAssumeType.name());
            } else {
                LOGGER.warn("Cannot set roleAssumeType to [{}] because roleBased map is null", roleAssumeType);
            }
        } else {
            LOGGER.warn("Cannot set roleAssumeType to [{}] because credential has no AWS parameter", roleAssumeType);
        }
    }

    public Boolean isGovernmentCloudEnabled() {
        Object govCloudEnabled = cloudCredential.getParameter(GOV_CLOUD, Object.class);
        if (govCloudEnabled instanceof Boolean) {
            return (Boolean) govCloudEnabled;
        } else if (govCloudEnabled instanceof String) {
            return Boolean.parseBoolean((String) govCloudEnabled);
        }
        return false;
    }

    public String getAccessKey() {
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> keyBased = getKeyBased();
            if (keyBased == null) {
                return null;
            }
            return keyBased.get(ACCESS_KEY);
        }
        return cloudCredential.getParameter(ACCESS_KEY, String.class);
    }

    public String getSecretKey() {
        if (cloudCredential.hasParameter(AWS)) {
            Map<String, String> keyBased = getKeyBased();
            if (keyBased == null) {
                return null;
            }
            return keyBased.get(SECRET_KEY);
        }
        return cloudCredential.getParameter(SECRET_KEY, String.class);
    }

    public Map<String, String> getKeyBased() {
        return (Map<String, String>) cloudCredential.getParameter(AWS, Map.class).get(KEY_BASED);
    }

    public Map<String, String> getRoleBased() {
        return (Map<String, String>) cloudCredential.getParameter(AWS, Map.class).get(ROLE_BASED);
    }

    public String getCredentialCrn() {
        return cloudCredential.getId();
    }

    public String getAccountId() {
        return cloudCredential.getAccountId();
    }

    public String getDefaultRegion() {
        return (String) cloudCredential.getParameter(AWS, Map.class).get(DEFAULT_REGION_KEY);
    }

    public boolean isSkipOrgPolicyDecisions() {
        return cloudCredential.getCredentialSettings().isSkipOrgPolicyDecisions();
    }

    @Override
    public String toString() {
        return "AwsCredentialView{" +
                "cloudCredential=" + cloudCredential +
                '}';
    }
}
