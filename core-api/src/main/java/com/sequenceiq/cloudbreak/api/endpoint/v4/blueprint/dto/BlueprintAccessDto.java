package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.dto;

import org.apache.commons.lang3.StringUtils;

public class BlueprintAccessDto {

    private final String name;

    private final String crn;

    public BlueprintAccessDto(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public static class BlueprintAccessDtoBuilder {

        private String name;

        private String crn;

        public static BlueprintAccessDtoBuilder aBlueprintAccessDtoBuilder() {
            return new BlueprintAccessDtoBuilder();
        }

        public BlueprintAccessDtoBuilder withName(String name) {
            if (StringUtils.isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public BlueprintAccessDtoBuilder withCrn(String crn) {
            if (StringUtils.isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public BlueprintAccessDto build() {
            return new BlueprintAccessDto(name, crn);
        }

    }

}
