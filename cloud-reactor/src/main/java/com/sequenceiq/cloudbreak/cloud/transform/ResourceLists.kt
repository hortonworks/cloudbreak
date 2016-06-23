package com.sequenceiq.cloudbreak.cloud.transform

import java.util.stream.Collectors

import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus

object ResourceLists {

    fun transform(cloudResourceStatuses: List<CloudResourceStatus>): List<CloudResource> {
        return cloudResourceStatuses.stream().map(Function<CloudResourceStatus, CloudResource> { it.getCloudResource() }).collect(Collectors.toList<CloudResource>())
    }
}
