package com.sequenceiq.environment.environment.dto;

public class FreeIpaCreationDto {

    private boolean create = true;

    private int instanceCountByGroup = 1;

    private FreeIpaCreationAwsParametersDto aws;

    private boolean enableMultiAz;

    private String imageCatalog;

    private String imageId;

    private FreeIpaCreationDto(Builder builder) {
        create = builder.create;
        instanceCountByGroup = builder.instanceCountByGroup;
        aws = builder.aws;
        imageCatalog = builder.imageCatalog;
        enableMultiAz = builder.enableMultiAz;
        imageId = builder.imageId;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean getCreate() {
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

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    @Override
    public String toString() {
        return "FreeIpaCreationDto{" +
            "create='" + create + '\'' +
            "instanceCountByGroup='" + instanceCountByGroup + '\'' +
            "aws='" + aws + '\'' +
            "imageCatalog='" + imageCatalog + '\'' +
            "imageId='" + imageId + '\'' +
            "enableMultiAz='" + enableMultiAz + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean create = true;

        private int instanceCountByGroup = 1;

        private FreeIpaCreationAwsParametersDto aws;

        private String imageCatalog;

        private String imageId;

        private boolean enableMultiAz;

        private Builder() {
        }

        public Builder withCreate(boolean create) {
            this.create = create;
            return this;
        }

        public Builder withInstanceCountByGroup(int instanceCountByGroup) {
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

        public Builder withEnableMultiAz(boolean enableMultiAz) {
            this.enableMultiAz = enableMultiAz;
            return this;
        }

        public FreeIpaCreationDto build() {
            return new FreeIpaCreationDto(this);
        }
    }
}
