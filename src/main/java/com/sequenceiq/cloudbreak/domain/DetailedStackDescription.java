package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class DetailedStackDescription extends StackDescription {
    private List<String> resources = new ArrayList<>();

    public DetailedStackDescription() {

    }

    @JsonRawValue
    public List<String> getResources() {
        return resources;
    }
}
