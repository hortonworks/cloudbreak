package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

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

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private String imageName;

    @Deprecated
    private Map<InstanceGroupType, String> userdata;

    private final String os;

    private final String osType;

    private final String imageCatalogUrl;

    private final String imageId;

    private final String imageCatalogName;

    private final Map<String, String> packageVersions;

    private final String date;

    private final Long created;

    @JsonCreator
    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata,
            @JsonProperty("os") String os,
            @JsonProperty("osType") String osType,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("packageVersions") Map<String, String> packageVersions,
            @JsonProperty("date") String date,
            @JsonProperty("created") Long created) {
        this.imageName = imageName;
        this.userdata = userdata != null ? ImmutableMap.copyOf(userdata) : null;
        this.imageCatalogUrl = imageCatalogUrl;
        this.os = os;
        this.osType = osType;
        this.imageCatalogName = imageCatalogName;
        this.imageId = imageId;
        this.packageVersions = packageVersions;
        this.date = date;
        this.created = created;
    }

    public String getImageName() {
        return imageName;
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

    public void setImageName(String imageName) {
        this.imageName = imageName;
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
                    && Objects.equals(imageCatalogUrl, image.imageCatalogUrl)
                    && Objects.equals(imageId, image.imageId)
                    && Objects.equals(imageCatalogName, image.imageCatalogName)
                    && Objects.equals(packageVersions, image.packageVersions)
                    && Objects.equals(date, image.date)
                    && Objects.equals(created, image.created);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, userdata, os, osType, imageCatalogUrl, imageId, imageCatalogName, packageVersions, date, created);
    }

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", packageVersions=" + packageVersions + '\''
                + ", date=" + date + '\''
                + ", created=" + created + '}';
    }
}
