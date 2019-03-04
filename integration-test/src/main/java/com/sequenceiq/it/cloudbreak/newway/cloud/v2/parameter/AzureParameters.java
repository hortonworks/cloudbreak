package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class AzureParameters {

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "HDP 3.1 - Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String PREFIX = CommonCloudParameters.PREFIX + "azure.";

    public static final String REGION = PREFIX + "region";

    public static final String LOCATION = PREFIX + "location";

    public static final String AVAILABILITY_ZONE = PREFIX + "availabilityZone";

    private AzureParameters() {
    }

    public static class Instance {

        private static final String PREFIX = AzureParameters.PREFIX + "instance.";

        public static final String TYPE = PREFIX + "type";

        public static final String VOLUME_SIZE = PREFIX + "volumeSize";

        public static final String VOLUME_COUNT = PREFIX + "volumeCount";

        public static final String VOLUME_TYPE = PREFIX + "volumeType";
    }

    public static class Credential {

        private static final String PREFIX = AzureParameters.PREFIX + "credential.";

        public static final String APP_ID = PREFIX + "appId";

        public static final String APP_PASSWORD = PREFIX + "appPassword";

        public static final String SUBSCRIPTION_ID = PREFIX + "subscriptionId";

        public static final String TENANT_ID = PREFIX + "tenantId";
    }
}
