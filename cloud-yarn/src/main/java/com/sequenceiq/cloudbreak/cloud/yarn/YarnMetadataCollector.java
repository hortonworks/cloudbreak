package com.sequenceiq.cloudbreak.cloud.yarn;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Container;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class YarnMetadataCollector implements MetadataCollector {
    @Inject
    private YarnClientUtil yarnClientUtil;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        try {
            YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
            CloudResource yarnApplication = getYarnApplcationResource(resources);
            ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
            applicationDetailRequest.setName(yarnApplication.getName());
            ResponseContext responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
            if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
                ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
                ListMultimap<String, CloudInstance> groupInstancesByInstanceGroup = groupInstancesByInstanceGroup(vms);
                ListMultimap<String, Container> groupContainersByInstanceGroup = groupContainersByInstanceGroup(applicationDetailResponse.getContainers());
                List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = Lists.newArrayList();
                for (String groupName : groupContainersByInstanceGroup.keySet()) {
                    List<CloudInstance> groupInstances = groupInstancesByInstanceGroup.get(groupName);
                    List<Container> groupContainers = groupContainersByInstanceGroup.get(groupName);
                    Map<String, CloudInstance> mapByInstanceId = mapByInstanceId(groupInstances);
                    Queue<CloudInstance> untrackedInstances = untrackedInstances(groupInstances);
                    for (Container container : groupContainers) {
                        String containerId = container.getId();
                        CloudInstance cloudInstance = mapByInstanceId.get(containerId);
                        if (cloudInstance == null) {
                            if (!untrackedInstances.isEmpty()) {
                                cloudInstance = untrackedInstances.remove();
                                cloudInstance = new CloudInstance(containerId, cloudInstance.getTemplate(), cloudInstance.getAuthentication());
                            }
                        }
                        if (cloudInstance != null) {
                            String ipAddress = container.getIp();
                            CloudInstanceMetaData md = new CloudInstanceMetaData(ipAddress, ipAddress);
                            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
                            cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
                        }
                    }
                }
                return cloudVmMetaDataStatuses;
            } else {
                ApplicationErrorResponse errorResponse = responseContext.getResponseError();
                throw new CloudConnectorException(String.format("Failed to get yarn application details: HTTP Return: %d Error: %s",
                        responseContext.getStatusCode(), errorResponse == null ? "unknown" : errorResponse.getDiagnostics()));
            }
        } catch (MalformedURLException ex) {
            throw new CloudConnectorException("Failed to get yarn application details", ex);
        }
    }

    private CloudResource getYarnApplcationResource(Iterable<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.YARN_APPLICATION) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.YARN_APPLICATION));
    }

    private ListMultimap<String, CloudInstance> groupInstancesByInstanceGroup(Iterable<CloudInstance> vms) {
        ListMultimap<String, CloudInstance> groupByInstanceGroup = ArrayListMultimap.create();
        for (CloudInstance vm : vms) {
            String groupName = vm.getTemplate().getGroupName();
            groupByInstanceGroup.put(groupName, vm);
        }
        return groupByInstanceGroup;
    }

    private ListMultimap<String, Container> groupContainersByInstanceGroup(Iterable<Container> containers) {
        ListMultimap<String, Container> groupByInstanceGroup = ArrayListMultimap.create();
        for (Container container : containers) {
            String groupName = container.getComponentName();
            groupByInstanceGroup.put(groupName, container);
        }
        return groupByInstanceGroup;
    }

    private Map<String, CloudInstance> mapByInstanceId(Iterable<CloudInstance> vms) {
        Map<String, CloudInstance> groupByInstanceId = Maps.newHashMap();
        for (CloudInstance vm : vms) {
            String instanceId = vm.getInstanceId();
            if (instanceId != null) {
                groupByInstanceId.put(instanceId, vm);
            }
        }
        return groupByInstanceId;
    }

    private Queue<CloudInstance> untrackedInstances(Iterable<CloudInstance> vms) {
        Queue<CloudInstance> cloudInstances = Lists.newLinkedList();
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                cloudInstances.add(vm);
            }
        }
        return cloudInstances;
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<String> loadBalancerTypes) {
        // no-op
        return Collections.emptyList();
    }
}
