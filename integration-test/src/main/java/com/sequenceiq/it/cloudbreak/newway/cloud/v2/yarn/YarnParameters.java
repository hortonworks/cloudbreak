package com.sequenceiq.it.cloudbreak.newway.cloud.v2.yarn;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;

public class YarnParameters {

    public static final String DEFAULT_LOCATION = "Frankfurt";

    public static final String DEFAULT_REGION = "default";

    public static final String DEFAULT_QUEUE = "HDP_2_6_0_0-integration-tests";

    public static final String DEFAULT_VOLUME_SIZE = "0";

    public static final String DEFAULT_VOLUME_COUNT = "0";

    public static final String DEFAULT_CPU_COUNT = "4";

    public static final String DEFAULT_MEMORY_SIZE = "8192";

    public static final String DEFAULT_IMAGE_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-dev-cb-image-catalog.json";

    public static final String DEFAULT_IMAGE_ID = "6c005903-57ca-4acf-bec7-6275069db34a";

    public static final String DEFAULT_ENDPOINT = "http://yprod001.l42scl.hortonworks.com:9191";

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

    public static class Image {

        private static final String PREFIX = YarnParameters.PREFIX + "image.";

        public static final String CATALOG = PREFIX + "catalog";

        public static final String CATALOG_URL = CATALOG + "url";

        public static final String ID = PREFIX + "id";
    }

    public static class Credential {

        private static final String YARN_PREFIX = YarnParameters.PREFIX + "credential.";

        public static final String ENDPOINT = YARN_PREFIX + "endpoint";
    }
}
