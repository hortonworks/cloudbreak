package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class CdpCredential implements Serializable {

    @JsonProperty("machineUserName")
    private String machineUserName;

    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("privateKey")
    private String privateKey;

    @JsonProperty("accessKeyType")
    private String accessKeyType;

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

    public String getAccessKeyType() {
        return accessKeyType;
    }

    public void setAccessKeyType(String accessKeyType) {
        this.accessKeyType = accessKeyType;
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNoneBlank(accessKey, privateKey);
    }

    @Override
    public String toString() {
        return "CdpCredential{" +
                "machineUserName='" + machineUserName + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", privateKey='*****" + '\'' +
                ", accessKeyType='" + accessKeyType + '\'' +
                '}';
    }
}
