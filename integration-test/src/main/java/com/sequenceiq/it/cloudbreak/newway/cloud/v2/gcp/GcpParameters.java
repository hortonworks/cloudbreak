package com.sequenceiq.it.cloudbreak.newway.cloud.v2.gcp;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;

public class GcpParameters {

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "HDP 3.1 - Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String PREFIX = CommonCloudParameters.PREFIX + "gcp.";

    public static final String AVAILABILITY_ZONE = PREFIX + "availabilityZone";

    public static final String REGION = PREFIX + "region";

    private GcpParameters() {
    }

    public static class Credential {

        private static final String PREFIX = GcpParameters.PREFIX + "credential.";

        public static final String TYPE = PREFIX + "type";

        public static final String JSON = PREFIX + "json";

        public static final String P12 = PREFIX + "p12";

        public static final String SERVICE_ACCOUNT_ID = PREFIX + "serviceAccountId";

        public static final String PROJECT_ID = PREFIX + "projectId";
    }

    public static class Instance {

        private static final String PREFIX = GcpParameters.PREFIX + "instance.";

        public static final String TYPE = PREFIX + "type";

        public static final String VOLUME_SIZE = PREFIX + "volumeSize";

        public static final String VOLUME_COUNT = PREFIX + "volumeCount";

        public static final String VOLUME_TYPE = PREFIX + "volumeType";
    }
}
