package com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter;

public class AwsParameters {

    private static final String PREFIX = CommonCloudParameters.PREFIX + "aws.";

    public static final String PUBLIC_KEY_ID = PREFIX + "publicKeyId";

    public static final String AVAILABILITY_ZONE = PREFIX + "availabilityZone";

    public static final String REGION = PREFIX + "region";

    public static final String VPC_ID = PREFIX + "vpcId";

    public static final String SUBNET_ID = PREFIX + "subnetId";

    private AwsParameters() {
    }

    public static class Instance {

        private static final String PREFIX = AwsParameters.PREFIX + "instance.";

        public static final String TYPE = PREFIX + "type";

        public static final String VOLUME_SIZE = PREFIX + "volumeSize";

        public static final String VOLUME_COUNT = PREFIX + "volumeCount";

        public static final String VOLUME_TYPE = PREFIX + "volumeType";
    }

    public static class Credential {

        private static final String PREFIX = AwsParameters.PREFIX + "credential.";

        public static final String TYPE = PREFIX + "type";

        public static final String ROLE_ARN = PREFIX + "roleArn";

        public static final String ACCESS_KEY_ID = PREFIX + "accessKeyId";

        public static final String SECRET_KEY = PREFIX + "secretKey";
    }
}
