package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.ForwardingRuleList;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpMetadataCollector.class);

    @Inject
    private GcpNetworkInterfaceProvider gcpNetworkInterfaceProvider;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private GcpInstanceProvider gcpInstanceProvider;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {

        List<CloudVmMetaDataStatus> instanceMetaData = new ArrayList<>();
        Map<String, CloudResource> instanceNameMap = groupByInstanceName(resources);
        Map<Long, CloudResource> privateIdMap = groupByPrivateId(resources);
        Map<String, Optional<NetworkInterface>> networkInterfacesByInstance = getNetworkInterfaceByInstance(authenticatedContext, instanceNameMap);

        for (CloudInstance cloudInstance : vms) {
            String instanceId = cloudInstance.getInstanceId();
            CloudResource cloudResource;
            cloudResource = instanceId != null ? instanceNameMap.get(instanceId) : privateIdMap.get(cloudInstance.getTemplate().getPrivateId());
            CloudVmMetaDataStatus cloudVmMetaDataStatus = getCloudVmMetaDataStatus(cloudResource, cloudInstance, networkInterfacesByInstance);
            instanceMetaData.add(cloudVmMetaDataStatus);
        }
        return instanceMetaData;
    }

    private Map<String, CloudResource> groupByInstanceName(Iterable<CloudResource> resources) {
        Map<String, CloudResource> instanceNameMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                instanceNameMap.put(resourceName, resource);
            }
        }
        return instanceNameMap;
    }

    private Map<Long, CloudResource> groupByPrivateId(Iterable<CloudResource> resources) {
        Map<Long, CloudResource> privateIdMap = new HashMap<>();
        for (CloudResource resource : resources) {
            if (GCP_INSTANCE == resource.getType()) {
                String resourceName = resource.getName();
                Long privateId = gcpStackUtil.getPrivateId(resourceName);
                if (privateId != null) {
                    privateIdMap.put(privateId, resource);
                }
            }
        }
        return privateIdMap;
    }

    private Map<String, Optional<NetworkInterface>> getNetworkInterfaceByInstance(AuthenticatedContext authenticatedContext,
            Map<String, CloudResource> instanceNameMap) {
        return gcpNetworkInterfaceProvider.provide(authenticatedContext, new ArrayList<>(instanceNameMap.values()));
    }

    private CloudVmMetaDataStatus getCloudVmMetaDataStatus(CloudResource cloudResource, CloudInstance matchedInstance,
            Map<String, Optional<NetworkInterface>> networkInterfacesByInstance) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus;
        if (cloudResource != null) {
            CloudInstance cloudInstance = new CloudInstance(
                    cloudResource.getName(),
                    matchedInstance.getTemplate(),
                    matchedInstance.getAuthentication(),
                    matchedInstance.getSubnetId(),
                    matchedInstance.getAvailabilityZone());
            Optional<NetworkInterface> networkInterface = networkInterfacesByInstance.get(cloudResource.getName());
            try {
                String privateIp = networkInterface
                        .map(NetworkInterface::getNetworkIP)
                        .orElseThrow(() -> new IOException("Private IP must not be null."));
                String publicIp = null;
                List<AccessConfig> acl = networkInterface.get().getAccessConfigs();
                if (acl != null && acl.get(0) != null) {
                    publicIp = networkInterface.get().getAccessConfigs().get(0).getNatIP();
                }
                CloudInstanceMetaData metaData = new CloudInstanceMetaData(privateIp, publicIp);
                CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, metaData);

            } catch (IOException e) {
                LOGGER.warn(String.format("Instance %s is not reachable", cloudResource.getName()), e);
                CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.UNKNOWN);
                cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA);
            }
        } else {
            CloudVmInstanceStatus status = new CloudVmInstanceStatus(matchedInstance, InstanceStatus.TERMINATED);
            cloudVmMetaDataStatus = new CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA);
        }
        return cloudVmMetaDataStatus;
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        CloudCredential credential = ac.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        String projectId = gcpStackUtil.getProjectId(credential);
        String region = ac.getCloudContext().getLocation().getRegion().getRegionName();
        List<CloudLoadBalancerMetadata> results = new ArrayList<>();
        Set<CloudResource> forwardingRules = resources.stream()
                .filter(resource -> resource.getType().equals(ResourceType.GCP_FORWARDING_RULE))
                .collect(Collectors.toSet());
        try {
            ForwardingRuleList forwardingRuleList = compute.forwardingRules().list(projectId, region).execute();
            if (forwardingRuleList.getWarning() != null) {
                LOGGER.warn("Warning fetching GCP loadbalancer metadata, {}", forwardingRuleList.getWarning().getMessage());
            }
            if (!forwardingRuleList.isEmpty() && forwardingRuleList.getItems() != null) {
                results.addAll(getLoadBalancerMetadata(loadBalancerTypes, forwardingRuleList, forwardingRules, compute, projectId, region));
            } else {
                LOGGER.info("No GCP load balancer forwarding rule has been returned for project id: '{}' in region '{}'", projectId, region);
            }
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Couldn't collect GCP LB metadata for {} ", projectId, e);
        }

        // no-op
        return results;
    }

    private List<CloudLoadBalancerMetadata> getLoadBalancerMetadata(List<LoadBalancerType> loadBalancerTypes, ForwardingRuleList forwardingRuleList,
            Set<CloudResource> forwardingRules, Compute compute, String projectId, String region) {
        Map<LoadBalancerMetadataKey, CloudLoadBalancerMetadata> loadBalancerMap = new LinkedHashMap<>();
        for (ForwardingRule forwardingRule : forwardingRuleList.getItems()) {
            LoadBalancerType loadBalancerType = gcpLoadBalancerTypeConverter.getScheme(forwardingRule.getLoadBalancingScheme()).getCbType();
            Optional<CloudResource> rule = forwardingRules.stream().filter(r -> r.getName().equals(forwardingRule.getName())).findFirst();
            if (rule.isPresent() && loadBalancerType == LoadBalancerType.PRIVATE &&
                    LoadBalancerTypeAttribute.GATEWAY_PRIVATE == rule.get().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class)) {
                LOGGER.debug("GATEWAY_PRIVATE LoadBalancer selected");
                loadBalancerType = LoadBalancerType.GATEWAY_PRIVATE;
            }
            if (rule.isPresent() && loadBalancerTypes.contains(loadBalancerType)) {
                String backendService = forwardingRule.getBackendService();
                BackendService service = getBackendService(compute, projectId, region, backendService);
                LoadBalancerMetadataKey lbKey = new LoadBalancerMetadataKey(loadBalancerType, forwardingRule.getIPAddress(), backendService);
                if (loadBalancerMap.containsKey(lbKey)) {
                    getParams(forwardingRule, service).forEach((key, value) -> loadBalancerMap.get(lbKey).putParameter(key, value));
                } else {
                    CloudLoadBalancerMetadata loadBalancerMetadata = CloudLoadBalancerMetadata.builder()
                            .withType(loadBalancerType)
                            .withIp(forwardingRule.getIPAddress())
                            .withName(backendService)
                            .withParameters(getParams(forwardingRule, service))
                            .build();
                    loadBalancerMap.put(lbKey, loadBalancerMetadata);
                }
            }
        }
        return new ArrayList<>(loadBalancerMap.values());
    }

    private Map<String, Object> getParams(ForwardingRule item, BackendService backendService) {
        Map<String, Object> params = new HashMap<>();
        List<String> ports = item.getPorts();
        if (backendService != null) {
            params.put(GcpLoadBalancerMetadataView.LOADBALANCER_NAME, backendService.getName());
            if (ports != null && !ports.isEmpty()) {
                String targetGroup = backendService.getBackends().get(0).getGroup();
                ports.forEach(port -> {
                    params.put(GcpLoadBalancerMetadataView.getBackendServiceParam(port), backendService.getName());
                    params.put(GcpLoadBalancerMetadataView.getInstanceGroupParam(port), targetGroup);
                });
            } else {
                LOGGER.warn("Unexpected port count on {}, {}", item.getName(), ports);
            }
        } else {
            LOGGER.warn("Couldn't deterimine instancegroups for {}", item.getName());
        }
        return params;
    }

    private BackendService getBackendService(Compute compute, String projectId, String region, String serviceName) {
        try {
            if (serviceName != null) {
                return compute.regionBackendServices().get(projectId, region,
                        serviceName.substring(serviceName.lastIndexOf("/") + 1)).execute();
            } else {
                LOGGER.warn("Cannot get backend service as service name is null");
                return null;
            }
        } catch (RuntimeException | IOException ex) {
            LOGGER.warn("Cannot get backend service for {}", serviceName, ex);
            return null;
        }
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return new InstanceStoreMetadata();
    }

    @Override
    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        CloudContext cloudContext = ac.getCloudContext();
        Map<String, String> instanceTypes = new HashMap<>();
        List<CloudResource> cloudResources = resourceRetriever.findAllByStatusAndTypeAndStack(CREATED, GCP_INSTANCE, cloudContext.getId());
        for (String instanceId : instanceIds) {
            CloudResource instanceResource = cloudResources.stream().filter(cloudResource -> cloudResource.getName().equals(instanceId))
                    .findFirst().orElse(null);
            if (instanceResource != null) {
                Optional<Instance> instanceOptional = gcpInstanceProvider.getInstance(ac, instanceResource.getName(), instanceResource.getAvailabilityZone());
                instanceOptional.ifPresent(instance -> instanceTypes.put(instance.getName(),
                        StringUtils.substringAfterLast(instance.getMachineType(), "/")));
            }
        }
        return new InstanceTypeMetadata(instanceTypes);
    }

    private record LoadBalancerMetadataKey(LoadBalancerType type, String ipAddress, String backend) {
    }
}
