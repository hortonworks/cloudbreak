package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;

public class ImageTestBuilder {

    private String date;

    private Long created;

    private Long published;

    private String description;

    private String os;

    private String osType;

    private String uuid;

    private String version;

    private Map<String, String> repo;

    private Map<String, Map<String, String>> imageSetsByProvider;

    private ImageStackDetails stackDetails;

    private boolean defaultImage;

    private Map<String, String> packageVersions;

    private List<List<String>> preWarmParcels;

    private List<String> preWarmCsd;

    private String cmBuildNumber;

    private boolean advertised;

    private String baseParcelUrl;

    private String sourceImageId;

    private Map<String, String> tags;

    public static ImageTestBuilder builder() {
        return new ImageTestBuilder();
    }

    public ImageTestBuilder withDate(String date) {
        this.date = date;
        return this;
    }

    public ImageTestBuilder withCreated(Long created) {
        this.created = created;
        return this;
    }

    public ImageTestBuilder withPublished(Long published) {
        this.published = published;
        return this;
    }

    public ImageTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ImageTestBuilder withOs(String os) {
        this.os = os;
        return this;
    }

    public ImageTestBuilder withOsType(String osType) {
        this.osType = osType;
        return this;
    }

    public ImageTestBuilder withUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public ImageTestBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    public ImageTestBuilder withRepo(Map<String, String> repo) {
        this.repo = repo;
        return this;
    }

    public ImageTestBuilder withImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider) {
        this.imageSetsByProvider = imageSetsByProvider;
        return this;
    }

    public ImageTestBuilder withStackDetails(ImageStackDetails stackDetails) {
        this.stackDetails = stackDetails;
        return this;
    }

    public ImageTestBuilder withDefaultImage(boolean defaultImage) {
        this.defaultImage = defaultImage;
        return this;
    }

    public ImageTestBuilder withPackageVersions(Map<String, String> packageVersions) {
        this.packageVersions = packageVersions;
        return this;
    }

    public ImageTestBuilder withPreWarmParcels(List<List<String>> preWarmParcels) {
        this.preWarmParcels = preWarmParcels;
        return this;
    }

    public ImageTestBuilder withPreWarmCsd(List<String> preWarmCsd) {
        this.preWarmCsd = preWarmCsd;
        return this;
    }

    public ImageTestBuilder withCmBuildNumber(String cmBuildNumber) {
        this.cmBuildNumber = cmBuildNumber;
        return this;
    }

    public ImageTestBuilder withAdvertised(boolean advertised) {
        this.advertised = advertised;
        return this;
    }

    public ImageTestBuilder withBaseParcelUrl(String baseParcelUrl) {
        this.baseParcelUrl = baseParcelUrl;
        return this;
    }

    public ImageTestBuilder withSourceImageId(String sourceImageId) {
        this.sourceImageId = sourceImageId;
        return this;
    }

    public ImageTestBuilder withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    public Image build() {
        return new Image(
                date,
                created,
                published,
                description,
                os,
                uuid,
                version,
                repo,
                imageSetsByProvider,
                stackDetails,
                osType,
                packageVersions,
                preWarmParcels,
                preWarmCsd,
                cmBuildNumber,
                advertised,
                baseParcelUrl,
                sourceImageId,
                tags
        );
    }
}
