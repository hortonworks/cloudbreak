package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;

@Component
@Order(1)
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureAffinityGroupResourceBuilder extends AzureSimpleNetworkResourceBuilder {
    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject azureDeleteContextObject, String region) throws Exception {
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_AFFINITY_GROUP;
    }
}
