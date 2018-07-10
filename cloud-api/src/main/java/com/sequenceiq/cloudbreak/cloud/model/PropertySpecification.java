package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertySpecification {

    @JsonProperty("Memory")
    private String memory;

    @JsonProperty("Cpu")
    private String cpu;

    @JsonProperty("Price")
    private Double price;

    @JsonProperty("encryption_supported")
    private Boolean encryptionSupported;

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getEncryptionSupported() {
        return encryptionSupported;
    }

    public void setEncryptionSupported(Boolean encryptionSupported) {
        this.encryptionSupported = encryptionSupported;
    }
}
