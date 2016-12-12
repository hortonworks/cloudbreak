package com.sequenceiq.it.spark.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.spark.ITResponse;

public class CloudVmInstanceStatuses extends ITResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudVmInstanceStatuses.class);

    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public CloudVmInstanceStatuses(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    private List<CloudVmInstanceStatus> createCloudVmInstanceStatuses() {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (String instanceId : instanceMap.keySet()) {
            cloudVmInstanceStatuses.add(instanceMap.get(instanceId).getCloudVmInstanceStatus());
        }
        return cloudVmInstanceStatuses;
    }

    @Override
    public Object handle(spark.Request request, spark.Response response) throws Exception {
        return createCloudVmInstanceStatuses();
    }
}