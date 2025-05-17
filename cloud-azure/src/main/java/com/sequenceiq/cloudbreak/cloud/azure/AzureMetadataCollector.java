package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.ARM_TEMPLATE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.NetworkInterface;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
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

    @Inject
    private AzureLoadBalancerMetadataCollector azureLbMetadataCollector;

    @Inject
    private AzurePlatformResources azurePlatformResources;

    @Inject
    private ResourceRetriever resourceRetriever;

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
        LOGGER.debug("Metadata collection finished with result {}", results);
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
        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());
        return vms.stream()
                .collect(Collectors.toMap(
                        vm -> {
                            if (vm.getInstanceId() != null) {
                                return vm.getInstanceId();
                            } else {
                                return azureUtils.getFullInstanceId(stackName, vm.getTemplate().getGroupName(), Long.toString(vm.getTemplate().getPrivateId()),
                                        vm.getDbIdOrDefaultIfNotExists());
                            }
                        },
                        CloudInstance::getTemplate));
    }

    @Override
    public List<CloudLoadBalancerMetadata> collectLoadBalancer(AuthenticatedContext ac, List<LoadBalancerType> loadBalancerTypes,
            List<CloudResource> resources) {
        LOGGER.debug("Collecting Azure load balancer metadata, for cluster {}", ac.getCloudContext().getName());

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = new ArrayList<>();
        String resourceGroup = azureUtils.getTemplateResource(resources).getName();
        String stackName = azureUtils.getStackName(ac.getCloudContext());
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        Optional<String> privateLoadBalancerName = Optional.empty();
        Optional<String> gatewayPrivateLoadBalancerName = Optional.empty();
        for (LoadBalancerType type : loadBalancerTypes) {
            String loadBalancerName = AzureLoadBalancer.getLoadBalancerName(type, stackName);
            if (LoadBalancerType.GATEWAY_PRIVATE == type) {
                gatewayPrivateLoadBalancerName = Optional.of(loadBalancerName);
                continue;
            }
            LOGGER.debug("Attempting to collect metadata for load balancer {}, type {}", loadBalancerName, type);
            try {
                Optional<String> ip;
                if (LoadBalancerType.PUBLIC.equals(type)) {
                    ip = lookupPublicIp(resourceGroup, azureClient, loadBalancerName);
                } else if (LoadBalancerType.PRIVATE.equals(type)) {
                    privateLoadBalancerName = Optional.of(loadBalancerName);
                    ip = lookupPrivateIp(resourceGroup, azureClient, loadBalancerName, LoadBalancerType.PRIVATE);
                } else {
                    ip = Optional.empty();
                }

                addCloudLoadBalancerMetadata(cloudLoadBalancerMetadata, ac, resourceGroup, loadBalancerName, loadBalancerName, type, ip);
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to find metadata for load balancer " + loadBalancerName, e);
            }
        }

        if (gatewayPrivateLoadBalancerName.isPresent()) {
            addGatewayPrivateLoadBalancer(cloudLoadBalancerMetadata, ac, resourceGroup, azureClient,
                    privateLoadBalancerName, gatewayPrivateLoadBalancerName.get());
        }

        return cloudLoadBalancerMetadata;
    }

    private void addCloudLoadBalancerMetadata(List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata,
            AuthenticatedContext ac, String resourceGroup, String loadBalancerNameAlias, String loadBalancerName, LoadBalancerType type, Optional<String> ip) {
        if (ip.isPresent()) {
            Map<String, Object> parameters = azureLbMetadataCollector.getParameters(ac, resourceGroup, loadBalancerName);
            CloudLoadBalancerMetadata loadBalancerMetadata = CloudLoadBalancerMetadata.builder()
                .withType(type)
                .withIp(ip.get())
                .withName(loadBalancerNameAlias)
                .withParameters(parameters)
                .build();
            cloudLoadBalancerMetadata.add(loadBalancerMetadata);
            LOGGER.debug("Saved metadata for load balancer: {}", loadBalancerMetadata);
        } else {
            LOGGER.warn("Unable to find metadata for load balancer {}.", loadBalancerName);
        }
    }

    private void addGatewayPrivateLoadBalancer(List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata, AuthenticatedContext ac, String resourceGroup,
            AzureClient azureClient, Optional<String> privateLoadBalancerName, String gatewayPrivateLoadBalancerName) {
        if (privateLoadBalancerName.isEmpty()) {
            LOGGER.warn("Gateway Private Load Balancer was needed but did not find private loadbalancer for the frontend discovery.");
        } else {
            try {
                Optional<String> ip = lookupPrivateIp(resourceGroup, azureClient, privateLoadBalancerName.get(), LoadBalancerType.GATEWAY_PRIVATE);
                addCloudLoadBalancerMetadata(cloudLoadBalancerMetadata, ac, resourceGroup,
                        gatewayPrivateLoadBalancerName, privateLoadBalancerName.get(), LoadBalancerType.GATEWAY_PRIVATE, ip);
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to find metadata for load balancer " + privateLoadBalancerName, e);
            }
        }
    }

    @Override
    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        return azurePlatformResources.collectInstanceStorageCount(ac, instanceTypes);
    }

    @Override
    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        CloudContext cloudContext = ac.getCloudContext();
        Optional<CloudResource> armTemplateResource = resourceRetriever.findByStatusAndTypeAndStack(CREATED, ARM_TEMPLATE, cloudContext.getId());
        String resourceGroupName = armTemplateResource
                .orElseThrow(() -> new CloudConnectorException(String.format("No ARM_TEMPALTE resource found: %s", cloudContext.getId()))).getName();
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        Map<String, VirtualMachine> virtualMachinesByName = azureVirtualMachineService.getVirtualMachinesByName(azureClient, resourceGroupName, instanceIds);
        Map<String, String> instanceTypes = virtualMachinesByName.values().stream()
                .collect(Collectors.toMap(vm -> StringUtils.substringAfterLast(vm.id(), "/"), vm -> vm.size().toString()));
        return new InstanceTypeMetadata(instanceTypes);
    }

    private Optional<String> lookupPrivateIp(String resourceGroup, AzureClient azureClient, String loadBalancerName, LoadBalancerType type) {
        List<AzureLoadBalancerFrontend> privateIpFrontends = azureClient.getLoadBalancerFrontends(resourceGroup, loadBalancerName, type).stream()
                .filter(fe -> type == fe.getLoadBalancerType()).collect(Collectors.toList());
        if (privateIpFrontends.isEmpty()) {
            LOGGER.warn("Unable to find private ip address for load balancer {}", loadBalancerName);
            return Optional.empty();
        } else {
            AzureLoadBalancerFrontend frontend = privateIpFrontends.get(0);
            if (privateIpFrontends.size() > 1) {
                LOGGER.warn("Found multiple private IPs ({}) for {} load balancer {}. Only one, {}, will be used.",
                        privateIpFrontends.stream().map(AzureLoadBalancerFrontend::getIp).collect(Collectors.joining(", ")),
                        type,
                        loadBalancerName,
                        frontend.getIp());
            }
            return Optional.ofNullable(frontend.getIp());
        }
    }

    private Optional<String> lookupPublicIp(String resourceGroup, AzureClient azureClient, String loadBalancerName) {
        List<AzureLoadBalancerFrontend> publicIpFrontends = azureClient.getLoadBalancerFrontends(resourceGroup, loadBalancerName, LoadBalancerType.PUBLIC);
        if (publicIpFrontends.isEmpty()) {
            LOGGER.warn("Unable to find public ip address for load balancer {}", loadBalancerName);
            return Optional.empty();
        } else {
            AzureLoadBalancerFrontend frontend = publicIpFrontends.get(0);
            if (publicIpFrontends.size() > 1) {
                LOGGER.warn("Found multiple public IPs ({}) for load balancer {}. Only one, {}, will be used.",
                        publicIpFrontends.stream().map(AzureLoadBalancerFrontend::getIp).collect(Collectors.joining(", ")),
                        loadBalancerName,
                        frontend.getIp());
            }
            return Optional.ofNullable(frontend.getIp());
        }
    }

    @Override
    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        return azureVirtualMachineService.collectCdpInstances(ac, resourceCrn, cloudStack, knownInstanceIds);
    }
}
