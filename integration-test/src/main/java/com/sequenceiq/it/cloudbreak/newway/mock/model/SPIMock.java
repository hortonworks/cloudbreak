package com.sequenceiq.it.cloudbreak.newway.mock.model;

import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;

import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;

import spark.Service;

public class SPIMock extends AbstractModelMock {

    public static final String TERMINATE_INSTANCES = "/terminate_instances";

    public static final String CLOUD_INSTANCE_STATUSES = "/cloud_instance_statuses";

    public static final String CLOUD_METADATA_STATUSES = "/cloud_metadata_statuses";

    public SPIMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService, defaultModel);
    }

    public void addSPIEndpoints() {
        Service sparkService = getSparkService();
        Map<String, CloudVmMetaDataStatus> instanceMap = getDefaultModel().getInstanceMap();
        postMockProviderMetadataStatus(sparkService, instanceMap);
        postMockProviderInstanceStatus(sparkService, instanceMap);
        postMockProviderTerminateInstance(sparkService, instanceMap);
    }

    private void postMockProviderTerminateInstance(Service sparkService, Map<String, CloudVmMetaDataStatus> instanceMap) {
        sparkService.post(MOCK_ROOT + TERMINATE_INSTANCES, (request, response) -> {
            List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
            }.getType());
            cloudInstances.forEach(cloudInstance -> getDefaultModel().terminateInstance(instanceMap, cloudInstance.getInstanceId()));
            return null;
        }, gson()::toJson);
    }

    private void postMockProviderInstanceStatus(Service sparkService, Map<String, CloudVmMetaDataStatus> instanceMap) {
        sparkService.post(MOCK_ROOT + CLOUD_INSTANCE_STATUSES, new CloudVmInstanceStatuses(instanceMap), gson()::toJson);
    }

    private void postMockProviderMetadataStatus(Service sparkService, Map<String, CloudVmMetaDataStatus> instanceMap) {
        sparkService.post(MOCK_ROOT + CLOUD_METADATA_STATUSES, new CloudMetaDataStatuses(instanceMap), gson()::toJson);
    }
}
