package com.sequenceiq.environment.environment.dto;

import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = FreeIpaCreationDto.Builder.class)
public class FreeIpaCreationDto {

    private boolean create = true;

    private int instanceCountByGroup;

    private FreeIpaCreationAwsParametersDto aws;

    private Set<String> recipes;

    private boolean enableMultiAz;

    private String imageCatalog;

    private String imageId;

    private String imageOs;

    private String instanceType;

    private FreeIpaCreationDto(Builder builder) {
        create = builder.create;
        instanceCountByGroup = builder.instanceCountByGroup;
        aws = builder.aws;
        imageCatalog = builder.imageCatalog;
        enableMultiAz = builder.enableMultiAz;
        imageId = builder.imageId;
        imageOs = builder.imageOs;
        instanceType = builder.instanceType;
        recipes = builder.recipes;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isCreate() {
        return create;
    }

    public void setInstanceCountByGroup(int instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public FreeIpaCreationAwsParametersDto getAws() {
        return aws;
    }

    public void setAws(FreeIpaCreationAwsParametersDto aws) {
        this.aws = aws;
    }

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageOs() {
        return imageOs;
    }

    public void setImageOs(String imageOs) {
        this.imageOs = imageOs;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Set<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<String> recipes) {
        this.recipes = recipes;
    }

    @Override
    public String toString() {
        return "FreeIpaCreationDto{" +
                "create=" + create +
                ", instanceCountByGroup=" + instanceCountByGroup +
                ", aws=" + aws +
                ", recipes=" + recipes +
                ", enableMultiAz=" + enableMultiAz +
                ", imageCatalog='" + imageCatalog + '\'' +
                ", imageId='" + imageId + '\'' +
                ", imageOs='" + imageOs + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }

    public static Builder builder(int instanceCountByGroup) {
        return new Builder(instanceCountByGroup);
    }

    @JsonPOJOBuilder
    public static class Builder {

        private boolean create = true;

        private Integer instanceCountByGroup;

        private FreeIpaCreationAwsParametersDto aws;

        private String imageCatalog;

        private String imageId;

        private String imageOs;

        private boolean enableMultiAz;

        private String instanceType;

        private Set<String> recipes;

        private Builder(int instanceCountByGroup) {
            this.instanceCountByGroup = instanceCountByGroup;
        }

        private Builder() {
        }

        public Builder withCreate(boolean create) {
            this.create = create;
            return this;
        }

        private Builder withInstanceCountByGroup(int instanceCountByGroup) {
            this.instanceCountByGroup = instanceCountByGroup;
            return this;
        }

        public Builder withAws(FreeIpaCreationAwsParametersDto aws) {
            this.aws = aws;
            return this;
        }

        public Builder withImageCatalog(String imageCatalog) {
            this.imageCatalog = imageCatalog;
            return this;
        }

        public Builder withImageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder withImageOs(String imageOs) {
            this.imageOs = imageOs;
            return this;
        }

        public Builder withEnableMultiAz(boolean enableMultiAz) {
            this.enableMultiAz = enableMultiAz;
            return this;
        }

        public Builder withInstanceType(String instanceType) {
            this.instanceType = instanceType;
            return this;
        }

        public Builder withRecipes(Set<String> recipes) {
            this.recipes = recipes;
            return this;
        }

        public FreeIpaCreationDto build() {
            return new FreeIpaCreationDto(this);
        }
    }
}
