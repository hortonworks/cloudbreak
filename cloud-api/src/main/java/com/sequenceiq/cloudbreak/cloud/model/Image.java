package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private final String os;

    private final String osType;

    private final String architecture;

    private final String imageCatalogUrl;

    private final String imageId;

    private final String imageCatalogName;

    private final Map<String, String> packageVersions;

    private final String date;

    private final Long created;

    private final Map<String, String> tags;

    private String imageName;

    @Deprecated
    private Map<InstanceGroupType, String> userdata;

    @JsonCreator
    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata,
            @JsonProperty("os") String os,
            @JsonProperty("osType") String osType,
            @JsonProperty("architecture") String architecture,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("packageVersions") Map<String, String> packageVersions,
            @JsonProperty("date") String date,
            @JsonProperty("created") Long created,
            @JsonProperty("tags") Map<String, String> tags) {
        this.imageName = imageName;
        this.userdata = userdata != null ? ImmutableMap.copyOf(userdata) : null;
        this.imageCatalogUrl = imageCatalogUrl;
        this.os = os;
        this.osType = osType;
        this.architecture = architecture;
        this.imageCatalogName = imageCatalogName;
        this.imageId = imageId;
        this.packageVersions = packageVersions;
        this.date = date;
        this.created = created;
        this.tags = tags;
    }

    public static ImageBuilder builder() {
        return new ImageBuilder();
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Map<InstanceGroupType, String> getUserdata() {
        return userdata;
    }

    @Deprecated
    public void setUserdata(Map<InstanceGroupType, String> userdata) {
        this.userdata = userdata;
    }

    public String getOsType() {
        return osType;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public String getOs() {
        return os;
    }

    public Map<String, String> getPackageVersions() {
        return packageVersions == null ? new HashMap<>() : packageVersions;
    }

    public String getPackageVersion(ImagePackageVersion packageVersion) {
        return getPackageVersions().get(packageVersion.getKey());
    }

    public String getDate() {
        return date;
    }

    public Long getCreated() {
        return created;
    }

    public String getArchitecture() {
        return architecture;
    }

    public Architecture getArchitectureEnum() {
        return Architecture.fromStringWithFallback(architecture);
    }

    public Map<String, String> getTags() {
        return tags != null ? tags : new HashMap<>();
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            Image image = (Image) o;
            return Objects.equals(imageName, image.imageName)
                    && Objects.equals(userdata, image.userdata)
                    && Objects.equals(os, image.os)
                    && Objects.equals(osType, image.osType)
                    && Objects.equals(architecture, image.architecture)
                    && Objects.equals(imageCatalogUrl, image.imageCatalogUrl)
                    && Objects.equals(imageId, image.imageId)
                    && Objects.equals(imageCatalogName, image.imageCatalogName)
                    && Objects.equals(packageVersions, image.packageVersions)
                    && Objects.equals(date, image.date)
                    && Objects.equals(created, image.created)
                    && Objects.equals(tags, image.tags);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, userdata, os, osType, architecture, imageCatalogUrl, imageId, imageCatalogName, packageVersions, date, created, tags);
    }

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", architecture='" + architecture + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", packageVersions=" + packageVersions + '\''
                + ", date=" + date + '\''
                + ", created=" + created + '\''
                + ", tags=" + tags + '}';
    }

    public static final class ImageBuilder {

        private String architecture = Architecture.X86_64.getName();

        private String imageName;

        private Map<InstanceGroupType, String> userdata = new EnumMap<>(InstanceGroupType.class);

        private String os;

        private String osType;

        private String imageCatalogUrl;

        private String imageId;

        private String imageCatalogName;

        private Map<String, String> packageVersions = new HashMap<>();

        private String date;

        private Long created;

        private Map<String, String> tags = new HashMap<>();

        private ImageBuilder() {
        }

        public ImageBuilder withArchitecture(String architecture) {
            this.architecture = architecture;
            return this;
        }

        public ImageBuilder withImageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public ImageBuilder withUserdata(Map<InstanceGroupType, String> userdata) {
            this.userdata = userdata;
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

        public ImageBuilder withImageCatalogUrl(String imageCatalogUrl) {
            this.imageCatalogUrl = imageCatalogUrl;
            return this;
        }

        public ImageBuilder withImageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public ImageBuilder withImageCatalogName(String imageCatalogName) {
            this.imageCatalogName = imageCatalogName;
            return this;
        }

        public ImageBuilder withPackageVersions(Map<String, String> packageVersions) {
            this.packageVersions = packageVersions;
            return this;
        }

        public ImageBuilder withDate(String date) {
            this.date = date;
            return this;
        }

        public ImageBuilder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public ImageBuilder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Image build() {
            return new Image(imageName, userdata, os, osType, architecture, imageCatalogUrl, imageCatalogName, imageId, packageVersions, date, created, tags);
        }
    }
}
