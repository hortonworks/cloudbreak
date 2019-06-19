package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class GeneralAccessDto {

    private final String name;

    private final String crn;

    public GeneralAccessDto(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public boolean isValid() {
        return isNotEmpty(name) ^ isNotEmpty(crn);
    }

}
