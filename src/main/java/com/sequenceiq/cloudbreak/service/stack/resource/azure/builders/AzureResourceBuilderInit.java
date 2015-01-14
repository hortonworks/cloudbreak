package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.TemplateGroup;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

@Component
public class AzureResourceBuilderInit implements
        ResourceBuilderInit<AzureProvisionContextObject, AzureDeleteContextObject, AzureStartStopContextObject> {

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public AzureProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureLocation azureLocation = AzureLocation.valueOf(stack.getRegion());

        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        AzureProvisionContextObject azureProvisionContextObject =
                new AzureProvisionContextObject(stack.getId(), credential.getCommonName(azureLocation), azureClient,
                        getOsImageName(credential, azureLocation), userData);
        return azureProvisionContextObject;
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
            List<TemplateGroup> templateGroups = Lists.newArrayList(stack.getTemplateGroups());
            resourceList.add(new Resource(ResourceType.AZURE_VIRTUAL_MACHINE, res, stack, templateGroups.get(0).getGroupName()));
            resourceList.add(new Resource(ResourceType.AZURE_SERVICE_CERTIFICATE, res, stack, templateGroups.get(0).getGroupName()));
            resourceList.add(new Resource(ResourceType.AZURE_CLOUD_SERVICE, res, stack, templateGroups.get(0).getGroupName()));
        }
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(azureLocation), azureClient, resourceList);
        return azureDeleteContextObject;
    }

    @Override
    public AzureStartStopContextObject startStopInit(Stack stack) throws Exception {
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        return new AzureStartStopContextObject(stack, azureClient);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getOsImageName(Credential credential, AzureLocation location) {
        return azureStackUtil.getOsImageName(credential, location);
    }

}
