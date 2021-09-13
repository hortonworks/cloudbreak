package com.sequenceiq.cloudbreak.cloud.context;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class LightHouseInitContext extends DynamicModel {

    private String subscriptionId;

    private CloudPlatform cloudPlatform;

    private String accountId;

    private String userCrn;

    public LightHouseInitContext(String subscriptionId, CloudPlatform cloudPlatform, String accountId, String userCrn) {
        this(new HashMap<>(), subscriptionId, cloudPlatform, accountId, userCrn);
    }

    public LightHouseInitContext(Map<String, Object> parameters, String subscriptionId, CloudPlatform cloudPlatform, String accountId, String userCrn) {
        super(parameters);
        this.subscriptionId = subscriptionId;
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.userCrn = userCrn;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    @Override
    public String toString() {
        return "LightHouseInitContext{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", cloudPlatform=" + cloudPlatform +
                ", accountId='" + accountId + '\'' +
                ", userCrn='" + userCrn + '\'' +
                '}';
    }

}