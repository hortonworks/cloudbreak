package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class DetailedGccStackDescription extends StackDescription {

    private List<String> disks = new ArrayList<>();
    private String network;
    private List<String> virtualMachines = new ArrayList<>();

    public DetailedGccStackDescription() {

    }
    @JsonRawValue
    public List<String> getDisks() {
        return disks;
    }

    public void setDisks(List<String> disks) {
        this.disks = disks;
    }

    @JsonRawValue
    public String getNetWork() {
        return network;
    }

    public void setNetwork(JsonNode network) {
        this.network = network.toString();
    }

    @JsonRawValue
    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
