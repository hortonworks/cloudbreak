package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class DetailedAzureStackDescription extends StackDescription {

    private String affinityGroup;
    private List<String> cloudServices = new ArrayList<>();
    private String storageAccount;
    private List<String> virtualMachines = new ArrayList<>();

    public DetailedAzureStackDescription() {

    }

    @JsonRawValue
    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(JsonNode affinityGroup) {
        this.affinityGroup = affinityGroup.toString();
    }

    @JsonRawValue
    public List<String> getCloudServices() {
        return cloudServices;
    }

    public void setCloudServices(List<String> cloudServices) {
        this.cloudServices = cloudServices;
    }

    @JsonRawValue
    public String getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(JsonNode storageAccount) {
        this.storageAccount = storageAccount.toString();
    }

    @JsonRawValue
    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
