package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public class Image {

    private String imageName;

    private Map<InstanceGroupType, String> userdata;

    public Image(String imageName) {
        this.imageName = imageName;
        userdata = new HashMap<>();
    }

    public String getImageName() {
        return imageName;
    }

    public String getUserData(InstanceGroupType key) {
        return userdata.get(key);
    }

    public void putUserData(InstanceGroupType key, String value) {
        userdata.put(key, value);
    }

}
