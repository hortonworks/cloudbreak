package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.GeneralAccessDto;

public class ClusterDefinitionAccessDto extends GeneralAccessDto {

    private ClusterDefinitionAccessDto(String name, String crn) {
        super(name, crn);
    }

    public static class ClusterDefinitionAccessDtoBuilder {

        private String name;

        private String crn;

        public static ClusterDefinitionAccessDtoBuilder aClusterDefinitionAccessDtoBuilder() {
            return new ClusterDefinitionAccessDtoBuilder();
        }

        public ClusterDefinitionAccessDtoBuilder withName(String name) {
            if (StringUtils.isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public ClusterDefinitionAccessDtoBuilder withCrn(String crn) {
            if (StringUtils.isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public ClusterDefinitionAccessDto build() {
            return new ClusterDefinitionAccessDto(name, crn);
        }

    }

}
