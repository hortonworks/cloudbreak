package com.sequenceiq.it.cloudbreak;

public class ResourceGroupTest {

    public static final String AZURE_RESOURCE_GROUP_USAGE_SINGLE = "SINGLE";

    public static final String AZURE_RESOURCE_GROUP_USAGE_SINGLE_DEDICATED = "SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT";

    public static final String AZURE_RESOURCE_GROUP_USAGE_MULTIPLE = "MULTIPLE";

    private ResourceGroupTest() {
    }

    public static boolean isSingleResourceGroup(String resourceGroupUsage) {
        return AZURE_RESOURCE_GROUP_USAGE_SINGLE.equals(resourceGroupUsage) ||
                AZURE_RESOURCE_GROUP_USAGE_SINGLE_DEDICATED.equals(resourceGroupUsage);
    }
}
