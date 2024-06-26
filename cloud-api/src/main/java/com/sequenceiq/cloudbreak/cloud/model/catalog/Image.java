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

    private static final String ARCHITECTURE = "architecture";

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

    private final String architecture;

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
            @JsonProperty(ARCHITECTURE) String architecture,
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
        this.architecture = architecture;
        this.tags = tags == null ? new HashMap<>() : tags;
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

    @JsonProperty(ARCHITECTURE)
    public String getArchitecture() {
        return architecture;
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
        return shortDescriptionFormat();
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
        if (!Objects.equals(architecture, image.architecture)) {
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
                architecture,
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

    public String shortDescriptionFormat() {
        return "Image{"
                + "uuid='" + uuid + '\''
                + ", os='" + os + '\''
                + ", architecture='" + architecture + '\''
                + '}';
    }

    public static ImageBuilder builder() {
        return new ImageBuilder();
    }

    public static final class ImageBuilder {

        private boolean advertised;

        private String architecture;

        private String baseParcelUrl;

        private String cmBuildNumber;

        private Long created;

        private String date;

        private boolean defaultImage;

        private String description;

        private Map<String, Map<String, String>> imageSetsByProvider;

        private String os;

        private String osType;

        private Map<String, String> packageVersions;

        private List<String> preWarmCsd;

        private List<List<String>> preWarmParcels;

        private Long published;

        private Map<String, String> repo;

        private String sourceImageId;

        private ImageStackDetails stackDetails;

        private Map<String, String> tags;

        private String uuid;

        private String version;

        private ImageBuilder() {
        }

        public ImageBuilder withAdvertised(boolean advertised) {
            this.advertised = advertised;
            return this;
        }

        public ImageBuilder withArchitecture(String architecture) {
            this.architecture = architecture;
            return this;
        }

        public ImageBuilder withBaseParcelUrl(String baseParcelUrl) {
            this.baseParcelUrl = baseParcelUrl;
            return this;
        }

        public ImageBuilder withCmBuildNumber(String cmBuildNumber) {
            this.cmBuildNumber = cmBuildNumber;
            return this;
        }

        public ImageBuilder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public ImageBuilder withDate(String date) {
            this.date = date;
            return this;
        }

        public ImageBuilder withDefaultImage(boolean defaultImage) {
            this.defaultImage = defaultImage;
            return this;
        }

        public ImageBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ImageBuilder withImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider) {
            this.imageSetsByProvider = imageSetsByProvider;
            return this;
        }

        public ImageBuilder withOs(String os) {
            this.os = os;
            return this;
        }

        public ImageBuilder withOsType(String osType) {
            this.osType = osType;
            return this;
        }

        public ImageBuilder withPackageVersions(Map<String, String> packageVersions) {
            this.packageVersions = packageVersions;
            return this;
        }

        public ImageBuilder withPreWarmCsd(List<String> preWarmCsd) {
            this.preWarmCsd = preWarmCsd;
            return this;
        }

        public ImageBuilder withPreWarmParcels(List<List<String>> preWarmParcels) {
            this.preWarmParcels = preWarmParcels;
            return this;
        }

        public ImageBuilder withPublished(Long published) {
            this.published = published;
            return this;
        }

        public ImageBuilder withRepo(Map<String, String> repo) {
            this.repo = repo;
            return this;
        }

        public ImageBuilder withSourceImageId(String sourceImageId) {
            this.sourceImageId = sourceImageId;
            return this;
        }

        public ImageBuilder withStackDetails(ImageStackDetails stackDetails) {
            this.stackDetails = stackDetails;
            return this;
        }

        public ImageBuilder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public ImageBuilder withUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public ImageBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        public ImageBuilder copy(Image image) {
            advertised = image.isAdvertised();
            architecture = image.getArchitecture();
            baseParcelUrl = image.getBaseParcelUrl();
            cmBuildNumber = image.getCmBuildNumber();
            created = image.getCreated();
            date = image.getDate();
            defaultImage = image.isDefaultImage();
            description = image.getDescription();
            imageSetsByProvider = image.getImageSetsByProvider();
            os = image.getOs();
            osType = image.getOsType();
            packageVersions = image.getPackageVersions();
            preWarmCsd = image.getPreWarmCsd();
            preWarmParcels = image.getPreWarmParcels();
            published = image.getPublished();
            repo = image.getRepo();
            sourceImageId = image.getSourceImageId();
            stackDetails = image.getStackDetails();
            tags = image.getTags();
            uuid = image.getUuid();
            version = image.getVersion();
            return this;
        }

        public Image build() {
            Image image = new Image(date, created, published, description, os, uuid, version, repo, imageSetsByProvider, stackDetails, osType, packageVersions,
                    preWarmParcels, preWarmCsd, cmBuildNumber, advertised, baseParcelUrl, sourceImageId, architecture, tags);
            image.setDefaultImage(defaultImage);
            return image;
        }
    }
}
