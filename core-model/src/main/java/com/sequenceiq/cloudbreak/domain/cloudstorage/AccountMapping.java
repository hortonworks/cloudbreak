package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.HashMap;
import java.util.Map;

public class AccountMapping {

    private Map<String, String> groupMappings = new HashMap<>();

    private Map<String, String> userMappings = new HashMap<>();

    public Map<String, String> getGroupMappings() {
        return groupMappings;
    }

    public void setGroupMappings(Map<String, String> groupMappings) {
        this.groupMappings = groupMappings == null ? new HashMap<>() : groupMappings;
    }

    public Map<String, String> getUserMappings() {
        return userMappings;
    }

    public void setUserMappings(Map<String, String> userMappings) {
        this.userMappings = userMappings == null ? new HashMap<>() : userMappings;
    }

}
