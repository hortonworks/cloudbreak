package com.sequenceiq.cloudbreak.cloud.cumulus.yarn;

import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.client.CumulusYarnClient;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util.CumulusYarnResourceNameHelper;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.common.api.type.ResourceType;

@org.springframework.stereotype.Service
public class CumulusYarnMetadataCollector implements MetadataCollector {
    @Inject
    private CumulusYarnClient client;

    @Inject
    private CumulusYarnResourceNameHelper cumulusYarnResourceNameHelper;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> allInstances) {
        try {
            DefaultApi api = client.createApi(authenticatedContext);

            List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();

            Map<String, CloudInstance> vmsCloudInstanceByInstanceId = getCloudInstanceByInstanceIdIfNotNull(vms);

            Map<String, CloudInstance> allCloudInstanceByInstanceId = getCloudInstanceByInstanceIdIfNotNull(allInstances);

            List<CloudResource> cloudResources = resources.stream()
                    .filter(cloudResource -> cloudResource.getType() == ResourceType.CUMULUS_YARN_SERVICE)
                    .collect(Collectors.toList());
            collectVmStatuses(vms, api, cloudVmMetaDataStatuses, vmsCloudInstanceByInstanceId, allCloudInstanceByInstanceId, cloudResources);
            return cloudVmMetaDataStatuses;
        } catch (ApiException e) {
            throw new CloudConnectorException("Failed to get yarn service details", e);
        }
    }

    private Map<String, CloudInstance> getCloudInstanceByInstanceIdIfNotNull(Collection<CloudInstance> cloudInstances) {
        return cloudInstances.stream()
                .filter(cloudInstance -> StringUtils.isNotBlank(cloudInstance.getInstanceId()))
                .collect(Collectors.toMap(CloudInstance::getInstanceId, cloudInstance -> cloudInstance, (o, o2) -> o));
    }

    private void collectVmStatuses(List<CloudInstance> vms, DefaultApi api, List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses,
            Map<String, CloudInstance> vmsCloudInstanceByInstanceId, Map<String, CloudInstance> allCloudInstanceByInstanceId, List<CloudResource> cloudResources)
            throws ApiException {
        for (CloudResource cloudResource : cloudResources) {
            ApiResponse<Service> response = api.appV1ServicesServiceNameGetWithHttpInfo(cloudResource.getName());
            Service service = response.getData();
            List<Component> components = service.getComponents();
            for (Component component : components) {
                collectVmStatusForComponent(vms, cloudVmMetaDataStatuses, vmsCloudInstanceByInstanceId, allCloudInstanceByInstanceId, component);
            }
        }
    }

    private void collectVmStatusForComponent(List<CloudInstance> vms, List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses,
            Map<String, CloudInstance> vmsCloudInstanceByInstanceId, Map<String, CloudInstance> allCloudInstanceByInstanceId, Component component) {
        if (!component.getContainers().isEmpty()) {
            List<CloudInstance> cloudInstancesForGroup = vms.stream()
                    .filter(cloudInstance ->
                            cumulusYarnResourceNameHelper.getComponentNameFromGroupName(cloudInstance.getTemplate().getGroupName())
                                    .equals(component.getName()))
                    .filter(cloudInstance -> cloudInstance.getInstanceId() == null)
                    .collect(Collectors.toList());
            addCloudVmMetaDataStatusFromContainer(cloudVmMetaDataStatuses, vmsCloudInstanceByInstanceId, component, cloudInstancesForGroup,
                    allCloudInstanceByInstanceId);
        }
    }

    private void addCloudVmMetaDataStatusFromContainer(List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses,
            Map<String, CloudInstance> vmsCloudInstanceByInstanceId, Component component, List<CloudInstance> cloudInstanceForGroup,
            Map<String, CloudInstance> allCloudInstanceByInstanceId) {
        for (Container container : component.getContainers()) {
            Optional<CloudInstance> cloudInstance = Optional.ofNullable(vmsCloudInstanceByInstanceId.get(container.getId()))
                    .or(() -> createNewCLoudInstanceForNewContainer(cloudInstanceForGroup, allCloudInstanceByInstanceId, container));
            addMetaDataIfCloudInstanceIsPresent(cloudVmMetaDataStatuses, container, cloudInstance);
        }
    }

    private void addMetaDataIfCloudInstanceIsPresent(List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses, Container container,
            Optional<CloudInstance> cloudInstance) {
        cloudInstance.ifPresent(instance -> {
            CloudInstanceMetaData md = new CloudInstanceMetaData(container.getIp(), container.getIp());
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(instance, InstanceStatus.CREATED);
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
            cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
        });
    }

    private Optional<? extends CloudInstance> createNewCLoudInstanceForNewContainer(List<CloudInstance> cloudInstanceForGroup,
            Map<String, CloudInstance> allCloudInstanceByInstanceId, Container container) {
        if (cloudInstanceForGroup.isEmpty() || allCloudInstanceByInstanceId.containsKey(container.getId())) {
            return Optional.empty();
        } else {
            CloudInstance instance = cloudInstanceForGroup.remove(0);
            return Optional.of(new CloudInstance(container.getId(), instance.getTemplate(), instance.getAuthentication(), instance.getParameters()));
        }
    }
}
