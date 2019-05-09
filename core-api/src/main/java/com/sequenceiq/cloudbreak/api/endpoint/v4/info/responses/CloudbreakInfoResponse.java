package com.sequenceiq.cloudbreak.api.endpoint.v4.info.responses;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakInfoResponse implements Serializable {

    private Map<String, Object> info;

    public CloudbreakInfoResponse() {
    }

    public Map<String, Object> getInfo() {
        return info;
    }

    public void setInfo(Map<String, Object> info) {
        this.info = info;
    }
}
