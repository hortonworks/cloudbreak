package com.sequenceiq.it.spark.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.it.spark.ITResponse;

public class CloudMetaDataStatuses extends ITResponse {

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    public CloudMetaDataStatuses(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    private List<CloudVmMetaDataStatus> createCloudVmMetaDataStatuses(List<CloudInstance> cloudInstances) {
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus oldCloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            InstanceTemplate oldTemplate = oldCloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
            Optional<CloudInstance> cloudInstance = cloudInstances.stream()
                    .filter(instance -> Objects.equals(instance.getTemplate().getPrivateId(), oldTemplate.getPrivateId())).findFirst();
            if (cloudInstance.isPresent()) {
                CloudInstance newCloudInstance = new CloudInstance(stringCloudVmMetaDataStatusEntry.getKey(), cloudInstance.get().getTemplate(),
                        cloudInstance.get().getParameters());
                CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance,
                        oldCloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus());
                CloudVmMetaDataStatus newCloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, oldCloudVmMetaDataStatus.getMetaData());
                cloudVmMetaDataStatuses.add(newCloudVmMetaDataStatus);
            }
        }
        return cloudVmMetaDataStatuses;
    }

    @Override
    public Object handle(spark.Request request, spark.Response response) throws Exception {
        List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
        }.getType());
        return createCloudVmMetaDataStatuses(cloudInstances);
    }
}