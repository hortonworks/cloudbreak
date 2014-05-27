package com.sequenceiq.provisioning.domain;

import java.util.ArrayList;
import java.util.List;

public class DetailedAzureStackDescription extends StackDescription {

    private String affinityGroup;
    private String cloudService;
    private String storageAccount;
    private List<String> virtualMachines = new ArrayList<>();

    public DetailedAzureStackDescription() {

    }

    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    public String getCloudService() {
        return cloudService;
    }

    public void setCloudService(String cloudService) {
        this.cloudService = cloudService;
    }

    public String getStorageAccount() {
        return storageAccount;
    }

    public void setStorageAccount(String storageAccount) {
        this.storageAccount = storageAccount;
    }

    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
