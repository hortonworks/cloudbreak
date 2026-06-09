package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.common.model.OsType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClusterUpgradeProperties(
        UpgradeRequestOptions options,
        CurrentImageUpgradeContext currentImage,
        TargetImageUpgradeContext targetImage) {

    @JsonCreator
    public ClusterUpgradeProperties(
            @JsonProperty("options") UpgradeRequestOptions options,
            @JsonProperty("currentImage") CurrentImageUpgradeContext currentImage,
            @JsonProperty("targetImage") TargetImageUpgradeContext targetImage) {
        this.options = options;
        this.currentImage = currentImage;
        this.targetImage = targetImage;
    }

    @JsonIgnore
    public UpgradeRequestOptions getOptions() {
        return options;
    }

    @JsonIgnore
    public CurrentImageUpgradeContext getCurrentImage() {
        return currentImage;
    }

    @JsonIgnore
    public TargetImageUpgradeContext getTargetImage() {
        return targetImage;
    }

    // TODO CB-33421: Remove flat accessors once callers use nested context records directly.
    // Flat accessors below delegate to nested context records for backwards compatibility with pre-grouping callers.

    @JsonIgnore
    public String getTargetImageId() {
        return targetImage.imageId();
    }

    @JsonIgnore
    public String targetImageId() {
        return getTargetImageId();
    }

    @JsonIgnore
    public String getImageCatalogName() {
        return targetImage.catalogName();
    }

    @JsonIgnore
    public String imageCatalogName() {
        return getImageCatalogName();
    }

    @JsonIgnore
    public String getImageCatalogUrl() {
        return targetImage.catalogUrl();
    }

    @JsonIgnore
    public String imageCatalogUrl() {
        return getImageCatalogUrl();
    }

    @JsonIgnore
    public boolean isReplaceVms() {
        return options.replaceVms();
    }

    @JsonIgnore
    public boolean replaceVms() {
        return isReplaceVms();
    }

    @JsonIgnore
    public boolean isLockComponents() {
        return options.lockComponents();
    }

    @JsonIgnore
    public boolean lockComponents() {
        return isLockComponents();
    }

    @JsonIgnore
    public boolean isRollingUpgradeEnabled() {
        return options.rollingUpgradeEnabled();
    }

    @JsonIgnore
    public boolean rollingUpgradeEnabled() {
        return isRollingUpgradeEnabled();
    }

    @JsonIgnore
    public String getRuntimeVersion() {
        return targetImage.runtimeVersion();
    }

    @JsonIgnore
    public String runtimeVersion() {
        return getRuntimeVersion();
    }

    @JsonIgnore
    public OsType getCurrentOsType() {
        return currentImage.osType();
    }

    @JsonIgnore
    public OsType currentOsType() {
        return getCurrentOsType();
    }

    @JsonIgnore
    public ClouderaManagerProduct getCdhParcel() {
        return targetImage.cdhParcel();
    }

    @JsonIgnore
    public ClouderaManagerProduct cdhParcel() {
        return getCdhParcel();
    }

    @JsonIgnore
    public Set<ClouderaManagerProduct> getPreWarmParcels() {
        return targetImage.preWarmParcels();
    }

    @JsonIgnore
    public ClouderaManagerRepo getClouderaManagerRepo() {
        return targetImage.clouderaManagerRepo();
    }

    @JsonIgnore
    public String getCurrentImageId() {
        return currentImage.imageId();
    }

    @JsonIgnore
    public String currentImageId() {
        return getCurrentImageId();
    }

    @JsonIgnore
    public String getCurrentImageCatalogName() {
        return currentImage.catalogName();
    }

    @JsonIgnore
    public String currentImageCatalogName() {
        return getCurrentImageCatalogName();
    }

    @JsonIgnore
    public String getCurrentRuntimeVersion() {
        return currentImage.runtimeVersion();
    }

    @JsonIgnore
    public String currentRuntimeVersion() {
        return getCurrentRuntimeVersion();
    }

    @JsonIgnore
    public Map<String, String> getCurrentPackageVersions() {
        return currentImage.packageVersions();
    }

    @JsonIgnore
    public OsType getTargetOsType() {
        return targetImage.osType();
    }

    @JsonIgnore
    public OsType targetOsType() {
        return getTargetOsType();
    }

    @JsonIgnore
    public String getTargetImageVersion() {
        return targetImage.imageVersion();
    }

    @JsonIgnore
    public String getCdhBuildNumber() {
        return targetImage.cdhBuildNumber();
    }

    @JsonIgnore
    public String cdhBuildNumber() {
        return getCdhBuildNumber();
    }

    @JsonIgnore
    public Map<String, String> getTargetPackageVersions() {
        return targetImage.packageVersions();
    }

    @JsonIgnore
    public Map<String, String> getTargetImageTags() {
        return targetImage.tags();
    }

    @JsonIgnore
    public String getTargetImageName() {
        return targetImage.imageName();
    }

    @JsonIgnore
    public String targetImageName() {
        return getTargetImageName();
    }

    @JsonIgnore
    public Set<ClouderaManagerProduct> getAllTargetProducts() {
        return targetImage.getAllProducts();
    }

    @JsonIgnore
    public com.sequenceiq.cloudbreak.cloud.model.Image toCloudImage() {
        return targetImage.toCloudImage();
    }

    @JsonIgnore
    public com.sequenceiq.cloudbreak.cloud.model.catalog.Image toTargetCatalogImage() {
        // TODO CB-33362: Remove once ParcelAvailabilityService accepts ClusterUpgradeProperties.
        return targetImage.toCatalogImage();
    }

    @JsonIgnore
    public com.sequenceiq.cloudbreak.cloud.model.Image toCurrentCloudImage() {
        // TODO CB-33421: Remove once callers accept ClusterUpgradeProperties instead of cloud Image.
        return currentImage.toCloudImage();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpgradeRequestOptions(
            boolean replaceVms,
            boolean lockComponents,
            boolean rollingUpgradeEnabled) {

        @JsonCreator
        public UpgradeRequestOptions(
                @JsonProperty("replaceVms") boolean replaceVms,
                @JsonProperty("lockComponents") boolean lockComponents,
                @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
            this.replaceVms = replaceVms;
            this.lockComponents = lockComponents;
            this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        }

        @JsonIgnore
        public boolean isReplaceVms() {
            return replaceVms;
        }

        @JsonIgnore
        public boolean isLockComponents() {
            return lockComponents;
        }

        @JsonIgnore
        public boolean isRollingUpgradeEnabled() {
            return rollingUpgradeEnabled;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentImageUpgradeContext(
            String imageId,
            String catalogName,
            String catalogUrl,
            String runtimeVersion,
            Map<String, String> packageVersions,
            Map<String, String> tags,
            OsType osType,
            String os,
            String architecture,
            String date,
            Long created,
            String imageName) {

        @JsonCreator
        public CurrentImageUpgradeContext(
                @JsonProperty("imageId") String imageId,
                @JsonProperty("catalogName") String catalogName,
                @JsonProperty("catalogUrl") String catalogUrl,
                @JsonProperty("runtimeVersion") String runtimeVersion,
                @JsonProperty("packageVersions") Map<String, String> packageVersions,
                @JsonProperty("tags") Map<String, String> tags,
                @JsonProperty("osType") OsType osType,
                @JsonProperty("os") String os,
                @JsonProperty("architecture") String architecture,
                @JsonProperty("date") String date,
                @JsonProperty("created") Long created,
                @JsonProperty("imageName") String imageName) {
            this.imageId = imageId;
            this.catalogName = catalogName;
            this.catalogUrl = catalogUrl;
            this.runtimeVersion = runtimeVersion;
            this.packageVersions = packageVersions;
            this.tags = tags;
            this.osType = osType;
            this.os = os;
            this.architecture = architecture;
            this.date = date;
            this.created = created;
            this.imageName = imageName;
        }

        @JsonIgnore
        public String getImageId() {
            return imageId;
        }

        @JsonIgnore
        public String getCatalogName() {
            return catalogName;
        }

        @JsonIgnore
        public String getCatalogUrl() {
            return catalogUrl;
        }

        @JsonIgnore
        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        @JsonIgnore
        public Map<String, String> getPackageVersions() {
            return packageVersions;
        }

        @JsonIgnore
        public Map<String, String> getTags() {
            return tags;
        }

        @JsonIgnore
        public OsType getOsType() {
            return osType;
        }

        @JsonIgnore
        public String getOs() {
            return os;
        }

        @JsonIgnore
        public String getArchitecture() {
            return architecture;
        }

        @JsonIgnore
        public String getDate() {
            return date;
        }

        @JsonIgnore
        public Long getCreated() {
            return created;
        }

        @JsonIgnore
        public String getImageName() {
            return imageName;
        }

        @JsonIgnore
        public com.sequenceiq.cloudbreak.cloud.model.Image toCloudImage() {
            return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                    .withImageName(imageName)
                    .withOs(os)
                    .withOsType(osType != null ? osType.getOsType() : null)
                    .withArchitecture(architecture)
                    .withImageCatalogUrl(catalogUrl)
                    .withImageCatalogName(catalogName)
                    .withImageId(imageId)
                    .withPackageVersions(packageVersions != null ? new HashMap<>(packageVersions) : new HashMap<>())
                    .withDate(date)
                    .withCreated(created)
                    .withTags(tags != null ? new HashMap<>(tags) : new HashMap<>())
                    .build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetImageUpgradeContext(
            String imageId,
            String catalogName,
            String catalogUrl,
            String runtimeVersion,
            String imageVersion,
            String cdhBuildNumber,
            Map<String, String> packageVersions,
            Map<String, String> tags,
            OsType osType,
            String os,
            String architecture,
            String date,
            Long created,
            String imageName,
            // TODO CB-33362: Remove once ParcelAvailabilityService no longer needs catalog Image reconstruction.
            ImageStackDetails stackDetails,
            Map<String, String> repo,
            List<List<String>> preWarmParcelEntries,
            List<String> preWarmCsd,
            ClouderaManagerProduct cdhParcel,
            Set<ClouderaManagerProduct> preWarmParcels,
            ClouderaManagerRepo clouderaManagerRepo) {

        @JsonCreator
        public TargetImageUpgradeContext(
                @JsonProperty("imageId") String imageId,
                @JsonProperty("catalogName") String catalogName,
                @JsonProperty("catalogUrl") String catalogUrl,
                @JsonProperty("runtimeVersion") String runtimeVersion,
                @JsonProperty("imageVersion") String imageVersion,
                @JsonProperty("cdhBuildNumber") String cdhBuildNumber,
                @JsonProperty("packageVersions") Map<String, String> packageVersions,
                @JsonProperty("tags") Map<String, String> tags,
                @JsonProperty("osType") OsType osType,
                @JsonProperty("os") String os,
                @JsonProperty("architecture") String architecture,
                @JsonProperty("date") String date,
                @JsonProperty("created") Long created,
                @JsonProperty("imageName") String imageName,
                @JsonProperty("stackDetails") ImageStackDetails stackDetails,
                @JsonProperty("repo") Map<String, String> repo,
                @JsonProperty("preWarmParcelEntries") List<List<String>> preWarmParcelEntries,
                @JsonProperty("preWarmCsd") List<String> preWarmCsd,
                @JsonProperty("cdhParcel") ClouderaManagerProduct cdhParcel,
                @JsonProperty("preWarmParcels") Set<ClouderaManagerProduct> preWarmParcels,
                @JsonProperty("clouderaManagerRepo") ClouderaManagerRepo clouderaManagerRepo) {
            this.imageId = imageId;
            this.catalogName = catalogName;
            this.catalogUrl = catalogUrl;
            this.runtimeVersion = runtimeVersion;
            this.imageVersion = imageVersion;
            this.cdhBuildNumber = cdhBuildNumber;
            this.packageVersions = packageVersions;
            this.tags = tags;
            this.osType = osType;
            this.os = os;
            this.architecture = architecture;
            this.date = date;
            this.created = created;
            this.imageName = imageName;
            this.stackDetails = stackDetails;
            this.repo = repo;
            this.preWarmParcelEntries = preWarmParcelEntries;
            this.preWarmCsd = preWarmCsd;
            this.cdhParcel = cdhParcel;
            this.preWarmParcels = preWarmParcels;
            this.clouderaManagerRepo = clouderaManagerRepo;
        }

        @JsonIgnore
        public String getImageId() {
            return imageId;
        }

        @JsonIgnore
        public String getCatalogName() {
            return catalogName;
        }

        @JsonIgnore
        public String getCatalogUrl() {
            return catalogUrl;
        }

        @JsonIgnore
        public String getRuntimeVersion() {
            return runtimeVersion;
        }

        @JsonIgnore
        public String getImageVersion() {
            return imageVersion;
        }

        @JsonIgnore
        public String getCdhBuildNumber() {
            return cdhBuildNumber;
        }

        @JsonIgnore
        public Map<String, String> getPackageVersions() {
            return packageVersions;
        }

        @JsonIgnore
        public Map<String, String> getTags() {
            return tags;
        }

        @JsonIgnore
        public OsType getOsType() {
            return osType;
        }

        @JsonIgnore
        public String getOs() {
            return os;
        }

        @JsonIgnore
        public String getArchitecture() {
            return architecture;
        }

        @JsonIgnore
        public String getDate() {
            return date;
        }

        @JsonIgnore
        public Long getCreated() {
            return created;
        }

        @JsonIgnore
        public String getImageName() {
            return imageName;
        }

        @JsonIgnore
        public ImageStackDetails getStackDetails() {
            return stackDetails;
        }

        @JsonIgnore
        public Map<String, String> getRepo() {
            return repo;
        }

        @JsonIgnore
        public List<List<String>> getPreWarmParcelEntries() {
            return preWarmParcelEntries;
        }

        @JsonIgnore
        public List<String> getPreWarmCsd() {
            return preWarmCsd;
        }

        @JsonIgnore
        public ClouderaManagerProduct getCdhParcel() {
            return cdhParcel;
        }

        @JsonIgnore
        public Set<ClouderaManagerProduct> getPreWarmParcels() {
            return preWarmParcels;
        }

        @JsonIgnore
        public ClouderaManagerRepo getClouderaManagerRepo() {
            return clouderaManagerRepo;
        }

        @JsonIgnore
        public Set<ClouderaManagerProduct> getAllProducts() {
            Set<ClouderaManagerProduct> products = new HashSet<>();
            if (cdhParcel != null) {
                products.add(cdhParcel);
            }
            if (preWarmParcels != null) {
                products.addAll(preWarmParcels);
            }
            return products;
        }

        @JsonIgnore
        public com.sequenceiq.cloudbreak.cloud.model.Image toCloudImage() {
            return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                    .withImageName(imageName)
                    .withOs(os)
                    .withOsType(osType != null ? osType.getOsType() : null)
                    .withArchitecture(architecture)
                    .withImageCatalogUrl(catalogUrl)
                    .withImageCatalogName(catalogName)
                    .withImageId(imageId)
                    .withPackageVersions(packageVersions != null ? new HashMap<>(packageVersions) : new HashMap<>())
                    .withDate(date)
                    .withCreated(created)
                    .withTags(tags != null ? new HashMap<>(tags) : new HashMap<>())
                    .build();
        }

        // TODO CB-33362: Remove once ParcelAvailabilityService accepts ClusterUpgradeProperties.
        @JsonIgnore
        public com.sequenceiq.cloudbreak.cloud.model.catalog.Image toCatalogImage() {
            return com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                    .withUuid(imageId)
                    .withVersion(imageVersion)
                    .withOs(os)
                    .withOsType(osType != null ? osType.getOsType() : null)
                    .withArchitecture(architecture)
                    .withPackageVersions(packageVersions != null ? new HashMap<>(packageVersions) : new HashMap<>())
                    .withTags(tags != null ? new HashMap<>(tags) : new HashMap<>())
                    .withDate(date)
                    .withCreated(created)
                    .withStackDetails(stackDetails)
                    .withRepo(repo != null ? new HashMap<>(repo) : new HashMap<>())
                    .withPreWarmParcels(preWarmParcelEntries)
                    .withPreWarmCsd(preWarmCsd)
                    .withCmBuildNumber(packageVersions != null
                            ? packageVersions.get(ImagePackageVersion.CM_BUILD_NUMBER.getKey()) : null)
                    .build();
        }
    }
}
