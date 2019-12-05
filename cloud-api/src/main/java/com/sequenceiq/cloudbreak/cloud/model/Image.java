package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.common.api.type.InstanceGroupType;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private final String imageName;

    private Map<InstanceGroupType, String> userdata;

    private final String os;

    private final String osType;

    private final String imageCatalogUrl;

    private final String imageId;

    private final String imageCatalogName;

    private final Map<String, String> packageVersions;

    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata,
            @JsonProperty("os") String os,
            @JsonProperty("osType") String osType,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("packageVersions") Map<String, String> packageVersions) {
        this.imageName = imageName;
        this.userdata = userdata != null ? ImmutableMap.copyOf(userdata) : null;
        this.imageCatalogUrl = imageCatalogUrl;
        this.os = os;
        this.osType = osType;
        this.imageCatalogName = imageCatalogName;
        this.imageId = imageId;
        this.packageVersions = packageVersions;
    }

    public String getImageName() {
        return imageName;
    }

    public String getUserDataByType(InstanceGroupType key) {
        return userdata.get(key);
    }

    public Map<InstanceGroupType, String> getUserdata() {
        return userdata;
    }

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

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", os='" + os + '\''
                + ", osType='" + osType + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", userdata=" + userdata + '\''
                + ", packageVersions=" + packageVersions + '}';
    }
}
