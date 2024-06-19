package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private static final String DATE = "date";

    private static final String CREATED = "created";

    private static final String PUBLISHED = "published";

    private static final String DESCRIPTION = "description";

    private static final String OS = "os";

    private static final String UUID = "uuid";

    private static final String VERSION = "version";

    private static final String REPO = "repo";

    private static final String IMAGES = "images";

    private static final String STACK_DETAILS = "stack-details";

    private static final String OS_TYPE = "os_type";

    private static final String PACKAGE_VERSIONS = "package-versions";

    private static final String PRE_WARM_PARCELS = "pre_warm_parcels";

    private static final String PRE_WARM_CSD = "pre_warm_csd";

    private static final String BUILD_NUMBER = "build-number";

    private static final String ADVERTISED = "advertised";

    private static final String TAGS = "tags";

    private final String date;

    private final Long created;

    private final Long published;

    private final String description;

    private final String os;

    private final String osType;

    private final String uuid;

    private final String version;

    private final Map<String, String> repo;

    private final Map<String, Map<String, String>> imageSetsByProvider;

    private final ImageStackDetails stackDetails;

    private boolean defaultImage;

    private final Map<String, String> packageVersions;

    private final List<List<String>> preWarmParcels;

    private final List<String> preWarmCsd;

    private final String cmBuildNumber;

    private final boolean advertised;

    private final String baseParcelUrl;

    private final String sourceImageId;

    private final Map<String, String> tags;

    @JsonCreator
    public Image(
            @JsonProperty(value = DATE, required = true) String date,
            @JsonProperty(value = CREATED) Long created,
            @JsonProperty(value = PUBLISHED) Long published,
            @JsonProperty(value = DESCRIPTION, required = true) String description,
            @JsonProperty(value = OS, required = true) String os,
            @JsonProperty(value = UUID, required = true) String uuid,
            @JsonProperty(VERSION) String version,
            @JsonProperty(REPO) Map<String, String> repo,
            @JsonProperty(value = IMAGES, required = true) Map<String, Map<String, String>> imageSetsByProvider,
            @JsonProperty(STACK_DETAILS) ImageStackDetails stackDetails,
            @JsonProperty(OS_TYPE) String osType,
            @JsonProperty(PACKAGE_VERSIONS) Map<String, String> packageVersions,
            @JsonProperty(PRE_WARM_PARCELS) List<List<String>> preWarmParcels,
            @JsonProperty(PRE_WARM_CSD) List<String> preWarmCsd,
            @JsonProperty(BUILD_NUMBER) String cmBuildNumber,
            @JsonProperty(ADVERTISED) boolean advertised,
            @JsonProperty("baseParcelUrl") String baseParcelUrl,
            @JsonProperty("sourceImageId") String sourceImageId,
            @JsonProperty(TAGS) Map<String, String> tags) {
        this.date = date;
        this.created = created;
        this.published = published;
        this.description = description;
        this.os = os;
        this.uuid = uuid;
        this.version = version;
        this.repo = (repo == null) ? Collections.emptyMap() : repo;
        this.imageSetsByProvider = imageSetsByProvider;
        this.stackDetails = stackDetails;
        this.osType = osType;
        this.packageVersions = packageVersions;
        this.preWarmParcels = preWarmParcels == null ? Collections.emptyList() : preWarmParcels;
        this.preWarmCsd = preWarmCsd == null ? Collections.emptyList() : preWarmCsd;
        this.cmBuildNumber = cmBuildNumber;
        this.advertised = advertised;
        this.baseParcelUrl = baseParcelUrl;
        this.sourceImageId = sourceImageId;
        this.tags = tags == null ? new HashMap<>() : tags;
    }

    public Image(Image that, Map<String, Map<String, String>> imageSetsByProvider) {
        this.date = that.date;
        this.created = that.created;
        this.published = that.published;
        this.description = that.description;
        this.os = that.os;
        this.uuid = that.uuid;
        this.version = that.version;
        this.repo = (that.repo == null) ? Collections.emptyMap() : that.repo;
        this.imageSetsByProvider = imageSetsByProvider;
        this.stackDetails = that.stackDetails;
        this.osType = that.osType;
        this.packageVersions = that.packageVersions;
        this.preWarmParcels = (that.preWarmParcels == null) ? Collections.emptyList() : that.preWarmParcels;
        this.preWarmCsd = (that.preWarmCsd == null) ? Collections.emptyList() : that.preWarmCsd;
        this.cmBuildNumber = that.cmBuildNumber;
        this.advertised = that.advertised;
        this.baseParcelUrl = that.baseParcelUrl;
        this.sourceImageId = that.sourceImageId;
        this.tags = that.tags == null ? new HashMap<>() : that.tags;
    }

    @JsonProperty(DATE)
    public String getDate() {
        return date;
    }

    @JsonProperty(DESCRIPTION)
    public String getDescription() {
        return description;
    }

    @JsonProperty(OS)
    public String getOs() {
        return os;
    }

    @JsonProperty(UUID)
    public String getUuid() {
        return uuid;
    }

    @JsonProperty(VERSION)
    public String getVersion() {
        return version;
    }

    @JsonProperty(REPO)
    public Map<String, String> getRepo() {
        return repo;
    }

    @JsonProperty(IMAGES)
    public Map<String, Map<String, String>> getImageSetsByProvider() {
        return imageSetsByProvider;
    }

    @JsonProperty(STACK_DETAILS)
    public ImageStackDetails getStackDetails() {
        return stackDetails;
    }

    @JsonProperty(OS_TYPE)
    public String getOsType() {
        return osType;
    }

    public void setDefaultImage(boolean defaultImage) {
        this.defaultImage = defaultImage;
    }

    @JsonIgnore
    public boolean isDefaultImage() {
        return defaultImage;
    }

    @JsonProperty(PACKAGE_VERSIONS)
    public Map<String, String> getPackageVersions() {
        return packageVersions == null ? new HashMap<>() : packageVersions;
    }

    public String getPackageVersion(ImagePackageVersion packageVersion) {
        return getPackageVersions().get(packageVersion.getKey());
    }

    @JsonIgnore
    public boolean isPrewarmed() {
        return stackDetails != null && stackDetails.getRepo() != null && stackDetails.getRepo().getStack() != null;
    }

    @JsonProperty(PRE_WARM_PARCELS)
    public List<List<String>> getPreWarmParcels() {
        return preWarmParcels;
    }

    @JsonProperty(PRE_WARM_CSD)
    public List<String> getPreWarmCsd() {
        return preWarmCsd;
    }

    @JsonProperty(CREATED)
    public Long getCreated() {
        return created;
    }

    @JsonProperty(PUBLISHED)
    public Long getPublished() {
        return published;
    }

    @JsonProperty(BUILD_NUMBER)
    public String getCmBuildNumber() {
        return cmBuildNumber;
    }

    @JsonProperty(ADVERTISED)
    public boolean isAdvertised() {
        return advertised;
    }

    @JsonProperty(TAGS)
    public Map<String, String> getTags() {
        return tags;
    }

    @JsonIgnore
    public String getBaseParcelUrl() {
        return baseParcelUrl;
    }

    @JsonIgnore
    public String getSourceImageId() {
        return sourceImageId;
    }

    @Override
    public String toString() {
        return shortOsDescriptionFormat();
    }

    @Override
    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity"})
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Image image = (Image) o;

        if (defaultImage != image.defaultImage) {
            return false;
        }
        if (advertised != image.advertised) {
            return false;
        }
        if (!Objects.equals(date, image.date)) {
            return false;
        }
        if (!Objects.equals(created, image.created)) {
            return false;
        }
        if (!Objects.equals(published, image.published)) {
            return false;
        }
        if (!Objects.equals(description, image.description)) {
            return false;
        }
        if (!Objects.equals(os, image.os)) {
            return false;
        }
        if (!Objects.equals(osType, image.osType)) {
            return false;
        }
        if (!Objects.equals(uuid, image.uuid)) {
            return false;
        }
        if (!Objects.equals(version, image.version)) {
            return false;
        }
        if (!Objects.equals(repo, image.repo)) {
            return false;
        }
        if (!Objects.equals(imageSetsByProvider, image.imageSetsByProvider)) {
            return false;
        }
        if (!Objects.equals(stackDetails, image.stackDetails)) {
            return false;
        }
        if (!Objects.equals(packageVersions, image.packageVersions)) {
            return false;
        }
        if (!Objects.equals(preWarmParcels, image.preWarmParcels)) {
            return false;
        }
        if (!Objects.equals(preWarmCsd, image.preWarmCsd)) {
            return false;
        }
        if (!Objects.equals(cmBuildNumber, image.cmBuildNumber)) {
            return false;
        }
        if (!Objects.equals(baseParcelUrl, image.baseParcelUrl)) {
            return false;
        }
        if (!Objects.equals(tags, image.tags)) {
            return false;
        }
        return Objects.equals(sourceImageId, image.sourceImageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(advertised,
                baseParcelUrl,
                cmBuildNumber,
                created,
                date,
                defaultImage,
                description,
                imageSetsByProvider,
                os,
                osType,
                packageVersions,
                preWarmCsd,
                preWarmParcels,
                published,
                repo,
                sourceImageId,
                stackDetails,
                uuid,
                version,
                tags);
    }

    public String shortOsDescriptionFormat() {
        return "Image{"
                + "uuid='" + uuid + '\''
                + ", os='" + os + '\''
                + '}';
    }
}
