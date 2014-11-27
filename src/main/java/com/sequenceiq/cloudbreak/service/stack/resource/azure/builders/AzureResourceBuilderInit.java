package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

@Component
public class AzureResourceBuilderInit implements
        ResourceBuilderInit<AzureProvisionContextObject, AzureDeleteContextObject, AzureDescribeContextObject, AzureStartStopContextObject> {

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public AzureProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient =  AzureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        AzureProvisionContextObject azureProvisionContextObject =
                new AzureProvisionContextObject(stack.getId(), credential.getCommonName(), azureClient,
                        getOsImageName(credential), userData);
        return azureProvisionContextObject;
    }

    @Override
    public AzureDeleteContextObject deleteInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient =  AzureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(), azureClient);
        return azureDeleteContextObject;
    }

    @Override
    public AzureDeleteContextObject decommissionInit(Stack stack, Set<String> decommisionSet) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient =  AzureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        List<Resource> resourceList = new ArrayList<>();
        for (String res : decommisionSet) {
            resourceList.add(new Resource(ResourceType.AZURE_VIRTUAL_MACHINE, res, stack));
            resourceList.add(new Resource(ResourceType.AZURE_SERVICE_CERTIFICATE, res, stack));
            resourceList.add(new Resource(ResourceType.AZURE_CLOUD_SERVICE, res, stack));
        }
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(), azureClient, resourceList);
        return azureDeleteContextObject;
    }

    @Override
    public AzureStartStopContextObject startStopInit(Stack stack) throws Exception {
        AzureClient azureClient = AzureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        return new AzureStartStopContextObject(stack, azureClient);
    }

    @Override
    public AzureDescribeContextObject describeInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = AzureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        AzureDescribeContextObject azureDescribeContextObject =
                new AzureDescribeContextObject(stack.getId(), credential.getCommonName(), azureClient);
        return azureDescribeContextObject;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getOsImageName(Credential credential) {
        return  azureStackUtil.getOsImageName(credential);
    }

}
