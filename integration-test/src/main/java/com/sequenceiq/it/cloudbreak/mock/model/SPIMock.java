package com.sequenceiq.it.cloudbreak.mock.model;

import static com.sequenceiq.it.cloudbreak.mock.ITResponse.MOCK_ROOT;

import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.cloudbreak.mock.spi.CloudVmInstanceStatuses;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;

import spark.Service;

public class SPIMock extends AbstractModelMock {

    public static final String TERMINATE_INSTANCES = "/terminate_instances";

    public static final String STOP_INSTANCE = "/:instanceid/stop";

    public static final String START_INSTANCE = "/:instanceid/start";

    public static final String CLOUD_INSTANCE_STATUSES = "/cloud_instance_statuses";

    public static final String CLOUD_METADATA_STATUSES = "/cloud_metadata_statuses";

    public static final String START_INSTANCES = "/start_instances";

    public static final String STOP_INSTANCES = "/stop_instances";

    private DynamicRouteStack dynamicRouteStack;

    public SPIMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService, defaultModel);
        dynamicRouteStack = new DynamicRouteStack(sparkService, defaultModel);
    }

    public void addSPIEndpoints() {
        Map<String, CloudVmMetaDataStatus> instanceMap = getDefaultModel().getInstanceMap();
        postMockProviderMetadataStatus(instanceMap);
        postMockProviderInstanceStatus(instanceMap);
        postMockProviderTerminateInstance(instanceMap);
        getMockProviderStopStatus();
        getMockProviderStartStatus();
        postMockProviderStartInstance(getDefaultModel());
        postMockProviderStopInstance(getDefaultModel());
    }

    public DynamicRouteStack getDynamicRouteStack() {
        return dynamicRouteStack;
    }

    private void getMockProviderStopStatus() {
        dynamicRouteStack.get(MOCK_ROOT + STOP_INSTANCE, (request, response) -> {
            String instanceid = request.params("instanceid");
            CloudInstance instance = new CloudInstance(instanceid, null, null);
            return new CloudVmInstanceStatus(instance, InstanceStatus.STOPPED);
        });
    }

    private void getMockProviderStartStatus() {
        dynamicRouteStack.get(MOCK_ROOT + START_INSTANCE, (request, response) -> {
            String instanceid = request.params("instanceid");
            CloudInstance instance = new CloudInstance(instanceid, null, null);
            return new CloudVmInstanceStatus(instance, InstanceStatus.STARTED);
        });
    }

    private void postMockProviderTerminateInstance(Map<String, CloudVmMetaDataStatus> instanceMap) {
        dynamicRouteStack.post(MOCK_ROOT + TERMINATE_INSTANCES, (request, response) -> {
            List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
            }.getType());
            cloudInstances.forEach(cloudInstance -> getDefaultModel().terminateInstance(instanceMap, cloudInstance.getInstanceId()));
            return null;
        });
    }

    private void postMockProviderInstanceStatus(Map<String, CloudVmMetaDataStatus> instanceMap) {
        dynamicRouteStack.post(MOCK_ROOT + CLOUD_INSTANCE_STATUSES, new CloudVmInstanceStatuses(instanceMap));
    }

    private void postMockProviderMetadataStatus(Map<String, CloudVmMetaDataStatus> instanceMap) {
        dynamicRouteStack.post(MOCK_ROOT + CLOUD_METADATA_STATUSES, new CloudMetaDataStatuses(instanceMap));
    }

    private void postMockProviderStartInstance(DefaultModel model) {
        dynamicRouteStack.post(MOCK_ROOT + START_INSTANCES, (request, response) -> {
            model.startAllInstances();
            return null;
        });
    }

    private void postMockProviderStopInstance(DefaultModel model) {
        dynamicRouteStack.post(MOCK_ROOT + STOP_INSTANCES, (request, response) -> {
            model.stopAllInstances();
            return null;
        });
    }
}
