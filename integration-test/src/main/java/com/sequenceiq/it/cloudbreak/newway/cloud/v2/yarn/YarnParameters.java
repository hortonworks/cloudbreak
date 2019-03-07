package com.sequenceiq.it.cloudbreak.newway.cloud.v2.yarn;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;

public class YarnParameters {

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "HDP 3.1 - Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String PREFIX = CommonCloudParameters.PREFIX + "yarn.";

    public static final String AVAILABILITY_ZONE = PREFIX + "availabilityZone";

    public static final String REGION = PREFIX + "region";

    public static final String LOCATION = PREFIX + "location";

    public static final String YARN_QUEUE = PREFIX + "queue";

    private YarnParameters() {
    }

    public static class Instance {

        private static final String PREFIX = YarnParameters.PREFIX + "instance.";

        public static final String CPU_COUNT = PREFIX + "cpus";

        public static final String MEMORY_SIZE = PREFIX + "memory";

        public static final String VOLUME_SIZE = PREFIX + "volumeSize";

        public static final String VOLUME_COUNT = PREFIX + "volumeCount";
    }

    public static class Credential {

        private static final String PREFIX = YarnParameters.PREFIX + "credential.";

        public static final String ENDPOINT = PREFIX + "endpoint";

        public static final String AMBARI_USER = PREFIX + "ambariUser";
    }
}
