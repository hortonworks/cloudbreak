package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class CommonCloudParameters {

    public static final String DEFAULT_CLUSTER_DEFINITION_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    public static final String DEFAULT_SSH_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBt"
            + "iTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofp"
            + "jT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh";

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting credential default description.";

    static final String PREFIX = "integrationtest.";

    public static final String SUBNET_CIDR = PREFIX + "subnetCidr";

    public static final String CLOUD_PROVIDER = PREFIX + "cloudProvider";

    public static final String GATEWAY_PORT = PREFIX + "gatewayPort";

    public static final String CLUSTER_DEFINITION_NAME = PREFIX + "clusterDefinitionName";

    public static final String SSH_PUBLIC_KEY = PREFIX + "sshPublicKey";

    private CommonCloudParameters() {
    }
}
