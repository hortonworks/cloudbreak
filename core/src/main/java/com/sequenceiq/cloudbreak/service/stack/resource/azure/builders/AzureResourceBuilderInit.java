package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureUpdateContextObject;

@Component
public class AzureResourceBuilderInit implements
        ResourceBuilderInit<AzureProvisionContextObject, AzureDeleteContextObject, AzureStartStopContextObject, AzureUpdateContextObject> {

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public AzureProvisionContextObject provisionInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());
        AzureProvisionContextObject azureProvisionContextObject =
                new AzureProvisionContextObject(stack.getId(), credential.getCommonName(azureLocation),
                        getOsImageName(credential, azureLocation, stack.getImage()), getAllocatedAddresses(stack));
        return azureProvisionContextObject;
    }

    @Override
    public AzureUpdateContextObject updateInit(Stack stack) {
        return new AzureUpdateContextObject(stack);
    }

    @Override
    public AzureDeleteContextObject deleteInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());

        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(azureLocation), azureClient);
        return azureDeleteContextObject;
    }

    @Override
    public AzureDeleteContextObject decommissionInit(Stack stack, Set<String> decommissionSet) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());

        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        List<Resource> resourceList = new ArrayList<>();
        for (String res : decommissionSet) {
            for (Resource resource : stack.getResources()) {
                if (resource.getResourceName().equals(res)) {
                    resourceList.add(resource);
                }
            }
        }
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(azureLocation), azureClient, resourceList);
        return azureDeleteContextObject;
    }

    @Override
    public AzureStartStopContextObject startStopInit(Stack stack) throws Exception {
        return new AzureStartStopContextObject(stack);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getOsImageName(Credential credential, AzureLocation location, String imageUrl) {
        return azureStackUtil.getOsImageName(credential, location, imageUrl);
    }

    private Set<String> getAllocatedAddresses(Stack stack) {
        List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
        Set<String> allocatedAddresses = new TreeSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                allocatedAddresses.add(instanceMetaData.getPrivateIp());
            }
        }
        return allocatedAddresses;
    }

}
