package com.sequenceiq.cloudbreak.cloud.transform;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class ResourceLists {

    public static List<CloudResource> transform(List<CloudResourceStatus> cloudResourceStatuses) {
        List<CloudResource> resources = Lists.transform(cloudResourceStatuses, new Function<CloudResourceStatus, CloudResource>() {

            @Nullable
            @Override
            public CloudResource apply(@Nullable CloudResourceStatus input) {
                return input.getCloudResource();
            }
        });
        return resources;
    }
}
