package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.GeneralAccessDto;

public class StackAccessDto extends GeneralAccessDto  {

    private StackAccessDto(String name, String crn) {
        super(name, crn);
    }

    public static StackAccessDtoBuilder builder() {
        return new StackAccessDtoBuilder();
    }

    public static class StackAccessDtoBuilder {

        private String name;

        private String crn;

        private StackAccessDtoBuilder() {
        }

        public StackAccessDtoBuilder withName(String name) {
            if (StringUtils.isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public StackAccessDtoBuilder withCrn(String crn) {
            if (StringUtils.isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public StackAccessDto build() {
            return new StackAccessDto(name, crn);
        }

    }

}
