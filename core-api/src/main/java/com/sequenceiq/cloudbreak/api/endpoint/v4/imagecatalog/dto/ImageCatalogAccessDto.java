package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.GeneralAccessDto;

public class ImageCatalogAccessDto extends GeneralAccessDto {

    private ImageCatalogAccessDto(String name, String crn) {
        super(name, crn);
    }

    public static class ImageCatalogAccessDtoBuilder {

        private String name;

        private String crn;

        public static ImageCatalogAccessDtoBuilder aImageCatalogAccessDtoBuilder() {
            return new ImageCatalogAccessDtoBuilder();
        }

        public ImageCatalogAccessDtoBuilder withName(String name) {
            if (StringUtils.isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public ImageCatalogAccessDtoBuilder withCrn(String crn) {
            if (StringUtils.isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public ImageCatalogAccessDto build() {
            return new ImageCatalogAccessDto(name, crn);
        }

    }

}
