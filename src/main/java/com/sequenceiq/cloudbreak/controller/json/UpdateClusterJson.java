package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class UpdateClusterJson implements JsonEntity {

    private Map<String, Integer> hosts = new HashMap<>();
    private StatusRequest status;

    public UpdateClusterJson() {

    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public Map<String, Integer> getHosts() {
        return hosts;
    }

    public void setHosts(Map<String, Integer> hosts) {
        this.hosts = hosts;
    }
}
