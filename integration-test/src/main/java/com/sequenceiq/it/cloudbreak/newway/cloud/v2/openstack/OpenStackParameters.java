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

    public static class PreWarmedTest {

        public static final String DEFAULT_HDP_VERSION = "3.1";

        public static final String DEFAULT_HDP_ENABLED = "true";

        public static final String DEFAULT_HDP_SCALE_GROUP = "compute";

        public static final String DEFAULT_HDP_GROUPS = "master,worker,compute";

        public static final String DEFAULT_HDF_CLUSTER_DEFINITION = "3.3";

        public static final String DEFAULT_HDF_ENABLED = "true";

        public static final String DEFAULT_HDF_SCALE_GROUP = "compute";

        public static final String DEFAULT_HDF_GROUPS = "master,worker,compute";

        private static final String PREFIX = OpenStackParameters.PREFIX + "prewarmed.";

        private static final String HDP = PREFIX + "hdp.";

        public static final String HDP_VERSION = HDP + "version";

        public static final String HDP_ENABLED = HDP + "enabled";

        public static final String HDP_GROUPS = HDP + "groups";

        public static final String HDP_SCALE_GROUP = HDP + "scaleGroup";

        private static final String HDF = PREFIX + "hdf.";

        public static final String HDF_CLUSTER_DEFINITION = HDF + "clusterDefinitionNames";

        public static final String HDF_ENABLED = HDF + "enabled";

        public static final String HDF_GROUPS = HDF + "groups";

        public static final String HDF_SCALE_GROUP = HDF + "scaleGroup";

    }
}
