package com.sequenceiq.cloudbreak.cloud;

public class PlatformParametersConsts {

    public static final String TTL_MILLIS = "timetolive";

    public static final String CUSTOM_INSTANCETYPE = "customInstanceType";

    public static final String CUSTOM_INSTANCETYPE_CPUS = "cpus";

    public static final String CUSTOM_INSTANCETYPE_MEMORY = "memory";

    public static final String NETWORK_IS_MANDATORY = "networkIsMandatory";

    public static final String UPSCALING_SUPPORTED = "upScalingSupported";

    public static final String DOWNSCALING_SUPPORTED = "downScalingSupported";

    public static final String STARTSTOP_SUPPORTED = "startStopSupported";

    public static final String REGIONS_SUPPORTED = "regionsSupported";

    public static final String VOLUME_ATTACHMENT_SUPPORTED = "volumeAttachmentSupported";

    public static final String VERTICAL_SCALING_SUPPORTED = "verticalScalingSupported";

    public static final String FREEIPA_STACK_TYPE = "freeipa";

    public static final String CLOUD_STACK_TYPE_PARAMETER = "cloudStackType";

    public static final String RESOURCE_GROUP_NAME_PARAMETER = "resourceGroupName";

    public static final String RESOURCE_GROUP_USAGE_PARAMETER = "resourceGroupUsage";

    public static final String RESOURCE_CRN_PARAMETER = "resourceCrn";

    public static final String ENCRYPTION_KEY_URL = "keyVaultUrl";

    public static final String ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "keyVaultResourceGroupName";

    private PlatformParametersConsts() {

    }
}
