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

    @Inject
    private AzureUtils azureUtils;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        final CloudResource resource = azureUtils.getTemplateResource(resources);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        List<InstanceTemplate> templates = Lists.transform(vms, CloudInstance::getTemplate);

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, from -> {
            return azureUtils.getPrivateInstanceId(resource.getName(), from.getGroupName(), Long.toString(from.getPrivateId()));
        });

        try {
            for (Map.Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                AzureClient azureClient = authenticatedContext.getParameter(AzureClient.class);
                VirtualMachine vm = azureClient.getVirtualMachine(resource.getName(), instance.getKey());

//                NetworkInterface networkInterface = vm.primaryNetworkInterface();

                NetworkInterface networkInterface = null;
                String privateIp = null;
                String publicIp = null;


                List<String> networkInterfaceIdList = vm.networkInterfaceIds();
                for (String networkInterfaceId: networkInterfaceIdList) {
                    networkInterface = azureClient.getNetworkInterfaceById(networkInterfaceId);
                    privateIp = networkInterface.primaryPrivateIp();
                    PublicIpAddress publicIpAddress = networkInterface.primaryIpConfiguration().getPublicIpAddress();
                    if (publicIpAddress == null || publicIpAddress.ipAddress() == null) {
                        publicIp = azureClient.getLoadBalancerIps(resource.getName(), azureUtils.getLoadBalancerId(resource.getName())).get(0);
                    } else {
                        publicIp = publicIpAddress.ipAddress();
                    }
                }

//                List<Map> network = (ArrayList<Map>) ((Map) ((Map) ((Map) azureClient.getVirtualMachine(resource.getName(), instance.getKey()))
//                        .get("properties"))
//                        .get("networkProfile"))
//                        .get("networkInterfaces");
//                String networkInterfaceName = getNameFromConnectionString(network.get(0).get("id").toString());
//                Map networkInterface = (Map) azureClient.getNetworkInterface(resource.getName(), networkInterfaceName);
//                List ips = (ArrayList) ((Map) networkInterface.get("properties")).get("ipConfigurations");
//                Map properties = (Map) ((Map) ips.get(0)).get("properties");
//                String publicIp;
//                if (properties.get("publicIPAddress") == null) {
//                    publicIp = azureClient.getLoadBalancerIp(resource.getName(), azureUtils.getLoadBalancerId(resource.getName()));
//                } else {
//                    Map publicIPAddress = (Map) properties.get("publicIPAddress");
//                    String publicIpName = publicIPAddress.get("id").toString();
//                    Map publicAdressObject = (Map) azureClient.getPublicIpAddress(resource.getName(), getNameFromConnectionString(publicIpName));
//                    Map publicIpProperties = (Map) publicAdressObject.get("properties");
//                    publicIp = publicIpProperties.get("ipAddress").toString();
//                }
//                String privateIp = properties.get("privateIPAddress").toString();
                String instanceId = instance.getKey();
                if (publicIp == null) {
                    throw new CloudConnectorException(String.format("Public ip address can not be null but it was on %s instance.", instance.getKey()));
                }
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

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
