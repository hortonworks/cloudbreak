package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataBusCredential implements Serializable {

    @JsonProperty("machineUserName")
    private String machineUserName;

    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("privateKey")
    private String privateKey;

    public String getMachineUserName() {
        return machineUserName;
    }

    public void setMachineUserName(String machineUserName) {
        this.machineUserName = machineUserName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
