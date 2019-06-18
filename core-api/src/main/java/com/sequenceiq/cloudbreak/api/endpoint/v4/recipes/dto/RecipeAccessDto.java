package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.GeneralAccessDto;

public class RecipeAccessDto extends GeneralAccessDto {

    private RecipeAccessDto(String name, String crn) {
        super(name, crn);
    }

    public static class RecipeAccessDtoBuilder {

        private String name;

        private String crn;

        public static RecipeAccessDtoBuilder aRecipeAccessDtoBuilder() {
            return new RecipeAccessDtoBuilder();
        }

        public RecipeAccessDtoBuilder withName(String name) {
            if (StringUtils.isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public RecipeAccessDtoBuilder withCrn(String crn) {
            if (StringUtils.isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public RecipeAccessDto build() {
            return new RecipeAccessDto(name, crn);
        }

    }

}
