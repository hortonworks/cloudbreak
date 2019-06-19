package com.sequenceiq.redbeams.dto;

public class Credential {

    private final String name;

    private final String attributes;

    private final String crn;

    public Credential(String name, String attributes, String crn) {
        this.name = name;
        this.attributes = attributes;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getAttributes() {
        return attributes;
    }

    public String getCrn() {
        return crn;
    }
}
