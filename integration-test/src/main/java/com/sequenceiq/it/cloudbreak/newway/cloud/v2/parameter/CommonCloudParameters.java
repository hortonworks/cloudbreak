package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class CommonCloudParameters {

    public static final String DEFAULT_CLUSTER_DEFINITION_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    public static final String DEFAULT_SSH_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDXF9paZtj6ibDgRGtw2jTls1fysxETxG/rbCN9vYlpDw6ROK90rjXn"
            + "C0qi43kXbs5Z/NyFDDcYW4U1nUmJCIhQqNJv00ukTbS4McfeNYrZF78KNLtbUP57SfKzjcH1Txz9Zr3ILEADqMyNRzgSB2gw5XnfbbLUFQYcF3hT11gjEw99Qi9eCQ6nCM37iy9t"
            + "CIfMyB5CI24LECv+8UMFdYw+X9JQXgkAlcPi1zmVNckD6dDGGC2nY9mK7jw0dqZ2W/Q2HvGVgP7iSxnKIFIylifPMz0jmtzpjPi4czgr34d4PFlQsv8LgwWEQMyTJGQmtF3GGa1o/"
            + "qT07gePY2tl1sAzOLszfglmkBZS+POYi65fxYGepF5C0Rmc3427dLp+HPl8QSAuq0j92/3LGLOKTjn1qC2MjBNhbkhFhDKnv0VQslmN/nkgDKUSHfQqfMwo4HXFIIydUWxT+5PxFC"
            + "aS4axzGQ2HYylxqonnU3P0DJkh/omegI36HyxxlMNlpf/zn3/zrwSFvKNCFvbQezJg8jdqq7VEHx4DH6WhYQ02TLsjmcA0EFp1HxTCbJojD/Ixev/Wc5duHotOBiS0CXdJwyzKSQt"
            + "tQS9NGn+/LvUyiD/Z/Rz2r3B0LpQsQu4tI/8F5Jq+QiRgS9cQRnuLZuvAwbSNYI+g+zPdhaZq8fvumWarcQ== apalfi@hortonworks.com\n";

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
