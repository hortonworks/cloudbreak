package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CloudStorageSupportedV4Responses {

    private Set<CloudStorageSupportedV4Response> cloudStorages = new HashSet<>();

    public Set<CloudStorageSupportedV4Response> getCloudStorages() {
        return cloudStorages;
    }

    public void setCloudStorages(Set<CloudStorageSupportedV4Response> cloudStorages) {
        this.cloudStorages = cloudStorages;
    }

    public static final CloudStorageSupportedV4Responses cloudStorageSupportedV4Responses(Set<CloudStorageSupportedV4Response> cloudStorages) {
        CloudStorageSupportedV4Responses cloudStorageSupportedV4Responses = new CloudStorageSupportedV4Responses();
        cloudStorageSupportedV4Responses.setCloudStorages(cloudStorages);
        return cloudStorageSupportedV4Responses;
    }
}
