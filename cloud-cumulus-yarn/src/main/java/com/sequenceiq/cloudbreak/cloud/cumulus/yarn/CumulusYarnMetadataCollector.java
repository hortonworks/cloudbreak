package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cb.yarn.service.api.ApiException;
import org.apache.cb.yarn.service.api.ApiResponse;
import org.apache.cb.yarn.service.api.impl.DefaultApi;
import org.apache.cb.yarn.service.api.records.Component;
import org.apache.cb.yarn.service.api.records.Container;
import org.apache.cb.yarn.service.api.records.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.client.CumulusYarnClient;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@org.springframework.stereotype.Service
public class CumulusYarnMetadataCollector implements MetadataCollector {
    @Inject
    private CumulusYarnClient client;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        try {
            DefaultApi api = client.createApi(authenticatedContext);

            List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();

            Map<String, CloudInstance> cloudInstanceByInstanceId =
                    vms.stream().collect(Collectors.toMap(CloudInstance::getInstanceId, cloudInstance -> cloudInstance, (o, o2) -> o));

            List<CloudResource> cloudResources = resources.stream()
                    .filter(cloudResource -> cloudResource.getType() == ResourceType.CUMULUS_YARN_SERVICE)
                    .collect(Collectors.toList());
            for (CloudResource cloudResource : cloudResources) {
                ApiResponse<Service> response = api.appV1ServicesServiceNameGetWithHttpInfo(cloudResource.getName());
                Service service = response.getData();
                List<Component> components = service.getComponents();
                for (Component component : components) {
                    if (!component.getContainers().isEmpty()) {
                        CloudInstance cloudInstanceForGroup = allInstances.stream()
                                .filter(cloudInstance ->
                                        cloudInstance.getTemplate().getGroupName().replaceAll("_", "-").equals(component.getName()))
                                .findAny()
                                .orElseThrow(() -> new CloudConnectorException(String.format("No CloudInstance for group [%s]", component.getName())));
                        addCloudVmMetaDataStatusFromContainer(cloudVmMetaDataStatuses, cloudInstanceByInstanceId, component, cloudInstanceForGroup);
                    }
                }

            }
            return cloudVmMetaDataStatuses;
        } catch (ApiException e) {
            throw new CloudConnectorException("Failed to get yarn service details", e);
        }
    }

    private void addCloudVmMetaDataStatusFromContainer(List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses,
            Map<String, CloudInstance> cloudInstanceByInstanceId, Component component, CloudInstance cloudInstanceForGroup) {
        for (Container container : component.getContainers()) {
            CloudInstance cloudInstance = Optional.ofNullable(cloudInstanceByInstanceId.get(container.getId()))
                    .orElse(new CloudInstance(container.getId(), cloudInstanceForGroup.getTemplate(),
                            cloudInstanceForGroup.getAuthentication()));
            CloudInstanceMetaData md = new CloudInstanceMetaData(container.getIp(), container.getIp());
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
            cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
        }
    }
}
