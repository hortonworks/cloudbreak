package com.sequenceiq.it.cloudbreak.mock.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

public class CloudVmInstanceStatuses extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    public CloudVmInstanceStatuses(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    public List<CloudVmInstanceStatus> createCloudVmInstanceStatuses() {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            cloudVmInstanceStatuses.add(stringCloudVmMetaDataStatusEntry.getValue().getCloudVmInstanceStatus());
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public Object handle(spark.Request request, spark.Response response) {
        return createCloudVmInstanceStatuses();
    }
}