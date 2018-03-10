package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Image {

    private final String imageName;

    private final Map<InstanceGroupType, String> userdata;

    private final String osType;

    private final String imageCatalogUrl;

    private final String imageId;

    private final String imageCatalogName;

    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata,
            @JsonProperty("osType") String osType,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageId") String imageId) {
        this.imageName = imageName;
        this.userdata = userdata != null ? ImmutableMap.copyOf(userdata) : null;
        this.imageCatalogUrl = imageCatalogUrl;
        this.osType = osType;
        this.imageCatalogName = imageCatalogName;
        this.imageId = imageId;
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

    @Override
    public String toString() {
        return "Image{"
                + "imageName='" + imageName + '\''
                + ", osType='" + osType + '\''
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageId='" + imageId + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + ", userdata=" + userdata + '}';
    }
}
