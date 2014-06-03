package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class AzureStackDescription extends StackDescription {
    private String cloudService;

    private List<String> virtualMachines = new ArrayList<>();

    public AzureStackDescription() {

    }

    @JsonRawValue
    public String getCloudService() {
        return cloudService;
    }

    public void setCloudService(JsonNode cloudService) {
        this.cloudService = cloudService.toString();
    }

    @JsonRawValue
    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
