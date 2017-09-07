package com.sequenceiq.it.spark.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.spark.ITResponse;

public class CloudVmInstanceStatuses extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    public CloudVmInstanceStatuses(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    private List<CloudVmInstanceStatus> createCloudVmInstanceStatuses() {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            cloudVmInstanceStatuses.add(stringCloudVmMetaDataStatusEntry.getValue().getCloudVmInstanceStatus());
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public Object handle(spark.Request request, spark.Response response) throws Exception {
        return createCloudVmInstanceStatuses();
    }
}