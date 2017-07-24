package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudSshKeys {

    private Map<String, Set<CloudSshKey>> cloudSshKeysResponses = new HashMap<>();

    public CloudSshKeys() {
    }

    public CloudSshKeys(Map<String, Set<CloudSshKey>> cloudSshKeysResponses) {
        this.cloudSshKeysResponses = cloudSshKeysResponses;
    }

    public Map<String, Set<CloudSshKey>> getCloudSshKeysResponses() {
        return cloudSshKeysResponses;
    }

    public void setCloudSshKeysResponses(Map<String, Set<CloudSshKey>> cloudSshKeysResponses) {
        this.cloudSshKeysResponses = cloudSshKeysResponses;
    }
}
