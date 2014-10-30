package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class DetailedGccStackDescription extends StackDescription {

    private List<String> resources = new ArrayList<>();

    public DetailedGccStackDescription() {

    }

    @JsonRawValue
    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

}
