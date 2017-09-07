package com.sequenceiq.cloudbreak.api.model.flex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexUsageControllerJson implements JsonEntity {

    private String guid;

    private String instanceId;

    private String region;

    private String provider;

    private String smartSenseId;

    private String userName;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSmartSenseId() {
        return smartSenseId;
    }

    public void setSmartSenseId(String smartSenseId) {
        this.smartSenseId = smartSenseId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
