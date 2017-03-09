package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIpAddress;
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
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        final CloudResource resource = azureUtils.getTemplateResource(resources);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        List<InstanceTemplate> templates = Lists.transform(vms, CloudInstance::getTemplate);

        String resourceName = resource.getName();
        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, from -> azureUtils.getPrivateInstanceId(resourceName,
                from.getGroupName(), Long.toString(from.getPrivateId())));

        try {
            for (Map.Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);
                VirtualMachine vm = azureClient.getVirtualMachine(resourceName, instance.getKey());

                NetworkInterface networkInterface = null;
                String privateIp = null;
                String publicIp = null;
                Integer faultDomainCount = azureClient.getFaultDomainNumber(resourceName, vm.name());
                String platform = authenticatedContext.getCloudContext().getPlatform().value();
                String location = authenticatedContext.getCloudContext().getLocation().getRegion().value();
                String hostgroupNm = AzureUtils.getGroupName(instance.getValue().getGroupName());
                StringBuilder localityIndicatorBuilder = new StringBuilder();
                localityIndicatorBuilder.append(LOCALITY_SEPARATOR)
                        .append(platform)
                        .append(LOCALITY_SEPARATOR)
                        .append(location)
                        .append(LOCALITY_SEPARATOR)
                        .append(resourceName)
                        .append(LOCALITY_SEPARATOR)
                        .append(hostgroupNm)
                        .append(LOCALITY_SEPARATOR)
                        .append(faultDomainCount);
                AzureUtils.removeBlankSpace(localityIndicatorBuilder);

                List<String> networkInterfaceIdList = vm.networkInterfaceIds();
                for (String networkInterfaceId: networkInterfaceIdList) {
                    networkInterface = azureClient.getNetworkInterfaceById(networkInterfaceId);
                    privateIp = networkInterface.primaryPrivateIp();
                    PublicIpAddress publicIpAddress = networkInterface.primaryIpConfiguration().getPublicIpAddress();
                    if (publicIpAddress == null || publicIpAddress.ipAddress() == null) {
                        publicIp = azureClient.getLoadBalancerIps(resourceName, azureUtils.getLoadBalancerId(resourceName)).get(0);
                    } else {
                        publicIp = publicIpAddress.ipAddress();
                    }
                }

                String instanceId = instance.getKey();
                if (publicIp == null) {
                    throw new CloudConnectorException(String.format("Public ip address can not be null but it was on %s instance.", instance.getKey()));
                }
                CloudInstanceMetaData md = new CloudInstanceMetaData(privateIp, publicIp, faultDomainCount == null ? null : localityIndicatorBuilder.toString());

                InstanceTemplate template = templateMap.get(instanceId);
                if (template != null) {
                    CloudInstance cloudInstance = new CloudInstance(instanceId, template);
                    CloudVmInstanceStatus status = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                    results.add(new CloudVmMetaDataStatus(status, md));
                }
            }

        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return results;
    }

}
