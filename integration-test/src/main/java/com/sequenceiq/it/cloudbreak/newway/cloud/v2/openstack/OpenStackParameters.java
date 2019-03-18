package com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack;

import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;

public class OpenStackParameters {

    public static final String DEFAULT_CLUSTER_DEFINTION_NAME = "HDP 3.1 - Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String PREFIX = CommonCloudParameters.PREFIX + "openstack.";

    public static final String AVAILABILITY_ZONE = PREFIX + "availabilityZone";

    public static final String REGION = PREFIX + "region";

    public static final String LOCATION = PREFIX + "location";

    public static final String PUBLIC_NET_ID = PREFIX + "publicNetId";

    public static final String NETWORKING_OPTION = PREFIX + "networkingOption";

    private OpenStackParameters() {
    }

    public static class Credential {

        private static final String PREFIX = OpenStackParameters.PREFIX + "credential.";

        public static final String ENDPOINT = PREFIX + "endpoint";

        public static final String TENANT = PREFIX + "tenant";

        public static final String USER_NAME = PREFIX + "userName";

        public static final String PASSWORD = PREFIX + "password";

    }

    public static class Instance {

        private static final String PREFIX = OpenStackParameters.PREFIX + "instance.";

        public static final String TYPE = PREFIX + "type";

        public static final String VOLUME_SIZE = PREFIX + "volumeSize";

        public static final String VOLUME_COUNT = PREFIX + "volumeCount";

        public static final String VOLUME_TYPE = PREFIX + "volumeType";
    }
}
