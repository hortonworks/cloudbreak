package com.sequenceiq.cloudbreak.service.image;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class ModelImageTestBuilder {

    private String imageName;

    private Map<InstanceGroupType, String> userdata;

    private String os;

    private String osType;

    private String imageCatalogUrl;

    private String imageId;

    private String imageCatalogName;

    private Map<String, String> packageVersions;

    private String date;

    private Long created;

    public static ModelImageTestBuilder builder() {
        return new ModelImageTestBuilder();
    }

    public ModelImageTestBuilder withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public ModelImageTestBuilder withUserData(Map<InstanceGroupType, String> userdata) {
        this.userdata = userdata;
        return this;
    }

    public ModelImageTestBuilder withOs(String os) {
        this.os = os;
        return this;
    }

    public ModelImageTestBuilder withOsType(String osType) {
        this.osType = osType;
        return this;
    }

    public ModelImageTestBuilder withImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
        return this;
    }

    public ModelImageTestBuilder withImageId(String imageId) {
        this.imageId = imageId;
        return this;
    }

    public ModelImageTestBuilder withImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
        return this;
    }

    public ModelImageTestBuilder withPackageVersions(Map<String, String> packageVersions) {
        this.packageVersions = packageVersions;
        return this;
    }

    public ModelImageTestBuilder withDate(String date) {
        this.date = date;
        return this;
    }

    public ModelImageTestBuilder withCreated(Long created) {
        this.created = created;
        return this;
    }

    public Image build() {
        return new Image(
                imageName,
                userdata,
                os,
                osType,
                imageCatalogUrl,
                imageCatalogName,
                imageId,
                packageVersions,
                date,
                created
        );
    }
}
