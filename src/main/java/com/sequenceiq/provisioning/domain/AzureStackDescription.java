package com.sequenceiq.provisioning.domain;

import java.util.ArrayList;
import java.util.List;

public class AzureStackDescription extends StackDescription {

    private String cloudService;

    private List<String> virtualMachines = new ArrayList<>();

    public AzureStackDescription() {

    }

    public String getCloudService() {
        return cloudService;
    }

    public void setCloudService(String cloudService) {
        this.cloudService = cloudService;
    }

    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
