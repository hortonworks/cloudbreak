package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.GeneralAccessDto;

public class BlueprintAccessDto extends GeneralAccessDto {

    private BlueprintAccessDto(String name, String crn) {
        super(name, crn);
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
