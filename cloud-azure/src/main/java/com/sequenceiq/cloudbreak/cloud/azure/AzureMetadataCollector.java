package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.Backend;
import com.microsoft.azure.management.network.InboundNatRule;
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

    @Inject
    private AzureUtils azureUtils;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        final CloudResource resource = azureUtils.getTemplateResource(resources);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        List<InstanceTemplate> templates = Lists.transform(vms, CloudInstance::getTemplate);

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, from -> azureUtils.getPrivateInstanceId(resource.getName(),
                from.getGroupName(), Long.toString(from.getPrivateId())));

        try {
            for (Map.Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);
                VirtualMachine vm = azureClient.getVirtualMachine(resource.getName(), instance.getKey());

                String privateIp = null;
                String publicIp = null;

                List<String> networkInterfaceIdList = vm.networkInterfaceIds();
                for (String networkInterfaceId : networkInterfaceIdList) {
                    NetworkInterface networkInterface = azureClient.getNetworkInterfaceById(networkInterfaceId);
                    privateIp = networkInterface.primaryPrivateIp();
                    PublicIpAddress publicIpAddress = networkInterface.primaryIpConfiguration().getPublicIpAddress();

                    List<Backend> backends = networkInterface.primaryIpConfiguration().listAssociatedLoadBalancerBackends();
                    List<InboundNatRule> inboundNatRules = networkInterface.primaryIpConfiguration().listAssociatedLoadBalancerInboundNatRules();

                    if (backends.size() > 0 || inboundNatRules.size() > 0) {
                        publicIp = azureClient.getLoadBalancerIps(resource.getName(), azureUtils.getLoadBalancerId(resource.getName())).get(0);
                    }

                    if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
                        publicIp = publicIpAddress.ipAddress();
                    }
                }

                String instanceId = instance.getKey();
                CloudInstanceMetaData md = new CloudInstanceMetaData(privateIp, publicIp);

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
