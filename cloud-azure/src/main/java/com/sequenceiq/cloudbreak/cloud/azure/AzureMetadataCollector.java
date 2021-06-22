package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class AzureMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMetadataCollector.class);

    private static final Character LOCALITY_SEPARATOR = '/';

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureVirtualMachineService azureVirtualMachineService;

    @Inject
    private AzureVmPublicIpProvider azureVmPublicIpProvider;

    @Override
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        LOGGER.debug("Starting to collect vm metadata.");
        List<CloudVmMetaDataStatus> results = new ArrayList<>();
        String resourceGroup = azureUtils.getTemplateResource(resources).getName();
        AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);

        Map<String, InstanceTemplate> templateMap = getTemplateMap(vms, authenticatedContext);
        Map<String, VirtualMachine> virtualMachinesByName = azureVirtualMachineService.getVirtualMachinesByName(azureClient, resourceGroup,
                templateMap.keySet());
        azureVirtualMachineService.refreshInstanceViews(virtualMachinesByName);
        try {
            for (Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                VirtualMachine vm = virtualMachinesByName.get(instance.getKey());
                //TODO: network interface is lazy, so we will fetch it for every instances
                if (vm != null) {
                    NetworkInterface networkInterface = vm.getPrimaryNetworkInterface();
                    String subnetId = networkInterface.primaryIPConfiguration().subnetName();

                    Integer faultDomainCount = azureClient.getFaultDomainNumber(resourceGroup, vm.name());

                    String publicIp = azureVmPublicIpProvider.getPublicIp(networkInterface);

                    String instanceId = instance.getKey();
                    String localityIndicator = Optional.ofNullable(faultDomainCount)
                            .map(domainCount -> getLocalityIndicator(domainCount, authenticatedContext.getCloudContext(), instance.getValue(), resourceGroup))
                            .orElse(null);
                    CloudInstanceMetaData md = new CloudInstanceMetaData(networkInterface.primaryPrivateIP(), publicIp, localityIndicator);

                    InstanceTemplate template = templateMap.get(instanceId);
                    if (template != null) {
                        Map<String, Object> params = new HashMap<>(1);
                        params.put(SUBNET_ID, subnetId);
                        params.put(CloudInstance.INSTANCE_NAME, vm.computerName());
                        CloudInstance cloudInstance = new CloudInstance(instanceId, template, null, subnetId, null, params);
                        CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                        results.add(new CloudVmMetaDataStatus(status, md));
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to collect vm metadata due to an exception: ", e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
        LOGGER.debug("Metadata collection finished");
        return results;
    }

    private String getLocalityIndicator(Integer faultDomainCount, CloudContext cloudContext, InstanceTemplate instanceTemplate, String resourceGroup) {
        String platform = cloudContext.getPlatform().value();
        String location = cloudContext.getLocation().getRegion().value();
        String hostgroupNm = instanceTemplate.getGroupName();
        StringBuilder localityIndicatorBuilder = new StringBuilder()
                .append(LOCALITY_SEPARATOR)
                .append(platform)
                .append(LOCALITY_SEPARATOR)
                .append(location)
                .append(LOCALITY_SEPARATOR)
                .append(resourceGroup)
                .append(LOCALITY_SEPARATOR)
                .append(hostgroupNm)
                .append(LOCALITY_SEPARATOR)
                .append(faultDomainCount);
        AzureUtils.removeBlankSpace(localityIndicatorBuilder);
        return localityIndicatorBuilder.toString();
    }

    private Map<String, InstanceTemplate> getTemplateMap(List<CloudInstance> vms, AuthenticatedContext authenticatedContext) {
        List<InstanceTemplate> templates = vms.stream().map(CloudInstance::getTemplate).collect(Collectors.toList());
        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());
        return templates.stream()
                .collect(Collectors.toMap(
                        template -> azureUtils.getPrivateInstanceId(stackName, template.getGroupName(), Long.toString(template.getPrivateId())),
                        template -> template));
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        LOGGER.debug("Collecting Azure load balancer metadata, for cluster {}", ac.getCloudContext().getName());

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = new ArrayList<>();
        String resourceGroup = azureUtils.getTemplateResource(resources).getName();
        final String stackName = azureUtils.getStackName(ac.getCloudContext());
        AzureClient azureClient = ac.getParameter(AzureClient.class);

        for (LoadBalancerType type : loadBalancerTypes) {
            String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(type, stackName);
            LOGGER.debug("Attempting to collect metadata for load balancer {}, type {}", loadBalancerName, type);
            try {
                Optional<String> ip;
                if (LoadBalancerType.PUBLIC.equals(type)) {
                    ip = lookupPublicIp(resourceGroup, azureClient, loadBalancerName);
                } else if (LoadBalancerType.PRIVATE.equals(type)) {
                    ip = lookupPrivateIp(resourceGroup, azureClient, loadBalancerName);
                } else {
                    ip = Optional.empty();
                }

                if (ip.isPresent()) {
                    CloudLoadBalancerMetadata loadBalancerMetadata = new CloudLoadBalancerMetadata.Builder()
                        .withType(type)
                        .withIp(ip.get())
                        .withName(loadBalancerName)
                        .build();
                    cloudLoadBalancerMetadata.add(loadBalancerMetadata);
                    LOGGER.debug("Saved metadata for load balancer: {}", loadBalancerMetadata);
                } else {
                    LOGGER.warn("Unable to find metadata for load balancer {}.", loadBalancerName);
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to find metadata for load balancer " + loadBalancerName, e);
            }
        }

        return cloudLoadBalancerMetadata;
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return new InstanceStoreMetadata();
    }

    private Optional<String> lookupPrivateIp(String resourceGroup, AzureClient azureClient, String loadBalancerName) {
        List<String> privateIps = azureClient.getLoadBalancerIps(resourceGroup, loadBalancerName, LoadBalancerType.PRIVATE);
        if (privateIps.isEmpty()) {
            LOGGER.warn("Unable to find private ip address for load balancer {}", loadBalancerName);
            return Optional.empty();
        } else {
            String ip = privateIps.get(0);
            if (privateIps.size() > 1) {
                LOGGER.warn("Found multiple private IPs ({}) for load balancer {}. Only one, {}, will be used.",
                        String.join(", ", privateIps),
                        loadBalancerName,
                        ip);
            }
            return Optional.ofNullable(ip);
        }
    }

    private Optional<String> lookupPublicIp(String resourceGroup, AzureClient azureClient, String loadBalancerName) {
        List<String> publicIps = azureClient.getLoadBalancerIps(resourceGroup, loadBalancerName, LoadBalancerType.PUBLIC);
        if (publicIps.isEmpty()) {
            LOGGER.warn("Unable to find public ip address for load balancer {}", loadBalancerName);
            return Optional.empty();
        } else {
            String ip = publicIps.get(0);
            if (publicIps.size() > 1) {
                LOGGER.warn("Found multiple public IPs ({}) for load balancer {}. Only one, {}, will be used.",
                        String.join(", ", publicIps),
                        loadBalancerName,
                        ip);
            }
            return Optional.ofNullable(ip);
        }
    }
}
