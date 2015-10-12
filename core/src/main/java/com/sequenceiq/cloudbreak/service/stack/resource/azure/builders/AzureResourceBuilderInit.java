package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureResourceBuilderInit implements ResourceBuilderInit<AzureDeleteContextObject> {

    @Inject
    private AzureStackUtil azureStackUtil;

    @Override
    public AzureDeleteContextObject deleteInit(Stack stack) throws Exception {
        AzureClient azureClient = azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        return new AzureDeleteContextObject(stack.getId(), azureClient);
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
