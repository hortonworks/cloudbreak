package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

import groovyx.net.http.HttpResponseException;

@Service
public class ArmMetadataCollector implements MetadataCollector {

    @Inject
    private ArmClient armClient;

    @Inject
    private ArmUtils armTemplateUtils;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        AzureRMClient access = armClient.createAccess(authenticatedContext.getCloudCredential());
        final CloudResource resource = armTemplateUtils.getTemplateResource(resources);
        List<CloudVmMetaDataStatus> results = new ArrayList<>();

        List<InstanceTemplate> templates = Lists.transform(vms, new Function<CloudInstance, InstanceTemplate>() {
            @Override
            public InstanceTemplate apply(CloudInstance input) {
                return input.getTemplate();
            }
        });

        Map<String, InstanceTemplate> templateMap = Maps.uniqueIndex(templates, new Function<InstanceTemplate, String>() {
            public String apply(InstanceTemplate from) {
                return armTemplateUtils.getPrivateInstanceId(resource.getName(), from.getGroupName(), Long.toString(from.getPrivateId()));
            }
        });

        try {
            for (Map.Entry<String, InstanceTemplate> instance : templateMap.entrySet()) {
                List<Map> network = (ArrayList<Map>) ((Map) ((Map) ((Map) access.getVirtualMachine(resource.getName(), instance.getKey()))
                        .get("properties"))
                        .get("networkProfile"))
                        .get("networkInterfaces");
                String networkInterfaceName = getNameFromConnectionString(network.get(0).get("id").toString());
                Map networkInterface = (Map) access.getNetworkInterface(resource.getName(), networkInterfaceName);
                List ips = (ArrayList) ((Map) networkInterface.get("properties")).get("ipConfigurations");
                Map properties = (Map) ((Map) ips.get(0)).get("properties");
                String publicIp = null;
                if (properties.get("publicIPAddress") == null) {
                    publicIp = access.getLoadBalancerIp(resource.getName(), armTemplateUtils.getLoadBalancerId(resource.getName()));
                } else {
                    Map publicIPAddress = (Map) properties.get("publicIPAddress");
                    String publicIpName = publicIPAddress.get("id").toString();
                    Map publicAdressObject = (Map) access.getPublicIpAddress(resource.getName(), getNameFromConnectionString(publicIpName));
                    Map publicIpProperties = (Map) publicAdressObject.get("properties");
                    publicIp = publicIpProperties.get("ipAddress").toString();
                }
                String privateIp = properties.get("privateIPAddress").toString();
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

        } catch (HttpResponseException e) {
            throw new CloudConnectorException(e.getResponse().getData().toString(), e);
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return results;
    }

    private String getNameFromConnectionString(String connection) {
        return connection.split("/")[connection.split("/").length - 1];
    }

}
