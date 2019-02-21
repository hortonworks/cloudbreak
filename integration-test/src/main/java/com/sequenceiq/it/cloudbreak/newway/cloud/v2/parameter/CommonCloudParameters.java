package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class CommonCloudParameters {

    public static final String DEFAULT_BLUEPRINT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    static final String PREFIX = "integrationtest.";

    public static final String SUBNET_CIDR = PREFIX + "subnetCidr";

    public static final String CLOUD_PROVIDER = PREFIX + "cloudprovider";

    public static final String GATEWAY_PORT = PREFIX + "gatewayPort";

    public static final String BLUEPRINT_NAME = PREFIX + "blueprintName";

    private CommonCloudParameters() {
    }
}
