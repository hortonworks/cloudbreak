package com.sequenceiq.cloudbreak.cloud.azure;



import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@Component
public class AzureResourceGroupMetadataProvider {

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    @Value("${cb.azure.image.store.resourcegroup:cloudbreak-images}")
    private String imageStoreResourceGroup;

    public String getStackName(CloudContext cloudContext) {
        return Splitter.fixedLength(maxResourceNameLength - cloudContext.getId().toString().length())
                .splitToList(cloudContext.getName()).get(0) + cloudContext.getId();
    }

    public String getResourceGroupName(CloudContext cloudContext, CloudStack cloudStack) {
        return cloudStack.getParameters().getOrDefault(RESOURCE_GROUP_NAME_PARAMETER, getDefaultResourceGroupName(cloudContext));
    }

    public String getResourceGroupName(CloudContext cloudContext, DatabaseStack databaseStack) {
        return databaseStack.getDatabaseServer().getParameters()
                .getOrDefault(RESOURCE_GROUP_NAME_PARAMETER, getDefaultResourceGroupName(cloudContext)).toString();
    }

    public String getResourceGroupName(CloudContext cloudContext, DynamicModel dynamicModel) {
        return dynamicModel.getParameters().getOrDefault(RESOURCE_GROUP_NAME_PARAMETER, getDefaultResourceGroupName(cloudContext)).toString();
    }

    public Boolean useSingleResourceGroup(CloudStack cloudStack) {
        String resourceGroupUsageParameter = cloudStack.getParameters().get(RESOURCE_GROUP_USAGE_PARAMETER);
        return ResourceGroupUsage.SINGLE.name().equals(resourceGroupUsageParameter) ? Boolean.TRUE : Boolean.FALSE;
    }

    public Boolean useSingleResourceGroup(DatabaseStack cloudStack) {
        String resourceGroupUsageParameter = cloudStack.getDatabaseServer().getParameters()
                .getOrDefault(RESOURCE_GROUP_USAGE_PARAMETER, "").toString();
        return ResourceGroupUsage.SINGLE.name().equals(resourceGroupUsageParameter) ? Boolean.TRUE : Boolean.FALSE;
    }

    public String getImageResourceGroupName(CloudContext cloudContext, CloudStack cloudStack) {
        Boolean useSingleResourceGroup = useSingleResourceGroup(cloudStack);
        if (useSingleResourceGroup) {
            return getResourceGroupName(cloudContext, cloudStack);
        } else {
            return imageStoreResourceGroup;
        }
    }

    private String getDefaultResourceGroupName(CloudContext cloudContext) {
        return getStackName(cloudContext);
    }
}
