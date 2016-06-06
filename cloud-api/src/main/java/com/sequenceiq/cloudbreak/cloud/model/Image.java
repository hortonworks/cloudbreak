package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class Image {

    private final String imageName;
    private final Map<InstanceGroupType, String> userdata;
    private final HDPRepo hdpRepo;
    private final String hdpVersion;

    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata,
            @JsonProperty("hdpRepo") HDPRepo hdpRepo,
            @JsonProperty("hdpVersion") String hdpVersion) {
        this.imageName = imageName;
        this.userdata = ImmutableMap.copyOf(userdata);
        this.hdpRepo = hdpRepo;
        this.hdpVersion = hdpVersion;
    }

    public String getImageName() {
        return imageName;
    }

    public String getUserData(InstanceGroupType key) {
        return userdata.get(key);
    }

    public Map<InstanceGroupType, String> getUserdata() {
        return userdata;
    }

    public HDPRepo getHdpRepo() {
        return hdpRepo;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }
}
