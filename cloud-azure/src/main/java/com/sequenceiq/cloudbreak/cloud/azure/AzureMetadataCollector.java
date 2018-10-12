package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerInboundNatRule;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Service
public class AzureMetadataCollector implements MetadataCollector {

    public static final Character LOCALITY_SEPARATOR = '/';

    @Inject
    private AzureUtils azureUtils;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        CloudResource resource = azureUtils.getTemplateResource(resources);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        List<InstanceTemplate> templates = Lists.transform(vms, CloudInstance::getTemplate);

        String stackName = azureUtils.getStackName(authenticatedContext.getCloudContext());
        String resourceGroupName = resource.getName();
        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates,
                from -> azureUtils.getPrivateInstanceId(stackName, from.getGroupName(), Long.toString(from.getPrivateId())));

        try {
            for (Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);
                VirtualMachine vm = azureClient.getVirtualMachine(resourceGroupName, instance.getKey());
                String subnetId = vm.getPrimaryNetworkInterface().primaryIPConfiguration().subnetName();
                String instanceName = vm.computerName();

                String privateIp = null;
                String publicIp = null;
                Integer faultDomainCount = azureClient.getFaultDomainNumber(resourceGroupName, vm.name());
                String platform = authenticatedContext.getCloudContext().getPlatform().value();
                String location = authenticatedContext.getCloudContext().getLocation().getRegion().value();
                String hostgroupNm = instance.getValue().getGroupName();
                StringBuilder localityIndicatorBuilder = new StringBuilder();
                localityIndicatorBuilder.append(LOCALITY_SEPARATOR)
                        .append(platform)
                        .append(LOCALITY_SEPARATOR)
                        .append(location)
                        .append(LOCALITY_SEPARATOR)
                        .append(resourceGroupName)
                        .append(LOCALITY_SEPARATOR)
                        .append(hostgroupNm)
                        .append(LOCALITY_SEPARATOR)
                        .append(faultDomainCount);
                AzureUtils.removeBlankSpace(localityIndicatorBuilder);

                List<String> networkInterfaceIdList = vm.networkInterfaceIds();
                for (String networkInterfaceId : networkInterfaceIdList) {
                    NetworkInterface networkInterface = azureClient.getNetworkInterfaceById(networkInterfaceId);
                    privateIp = networkInterface.primaryPrivateIP();
                    PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();

                    List<LoadBalancerBackend> backends = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerBackends();
                    List<LoadBalancerInboundNatRule> inboundNatRules = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerInboundNatRules();

                    if (!backends.isEmpty() || !inboundNatRules.isEmpty()) {
                        publicIp = azureClient.getLoadBalancerIps(resource.getName(), azureUtils.getLoadBalancerId(resource.getName())).get(0);
                    }

                    if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
                        publicIp = publicIpAddress.ipAddress();
                    }
                }

                String instanceId = instance.getKey();
                CloudInstanceMetaData md = new CloudInstanceMetaData(privateIp, publicIp, faultDomainCount == null ? null : localityIndicatorBuilder.toString());

                InstanceTemplate template = templateMap.get(instanceId);
                if (template != null) {
                    Map<String, Object> params = new HashMap<>(1);
                    params.put(CloudInstance.SUBNET_ID, subnetId);
                    params.put(CloudInstance.INSTANCE_NAME, instanceName);
                    CloudInstance cloudInstance = new CloudInstance(instanceId, template, null, params);
                    CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                    results.add(new CloudVmMetaDataStatus(status, md));
                }
            }
        } catch (RuntimeException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return results;
    }

}
