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

    public Image(@JsonProperty("imageName") String imageName,
            @JsonProperty("userdata") Map<InstanceGroupType, String> userdata) {
        this.imageName = imageName;
        this.userdata = ImmutableMap.copyOf(userdata);
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
}
