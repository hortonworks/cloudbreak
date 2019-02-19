package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class CommonCloudParameters {

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    static final String PREFIX = "integrationtest.";

    public static final String SUBNET_CIDR = PREFIX + "subnetCidr";

    public static final String CLOUD_PROVIDER = PREFIX + "cloudprovider";

    public static final String GATEWAY_PORT = PREFIX + "gatewayPort";

    public static final String CLUSTER_DEFINITION_NAME = PREFIX + "clusterDefinitionName";

    private CommonCloudParameters() {
    }
}
