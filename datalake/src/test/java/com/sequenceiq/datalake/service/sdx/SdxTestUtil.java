package com.sequenceiq.datalake.service.sdx;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;

public class SdxTestUtil {

    public static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    public static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    public static final Long SDX_ID = 2L;

    public static final String SDX_CRN = "crn";

    public static final String CLUSTER_NAME = "test-sdx-cluster";

    public static final String ENVIRONMENT_NAME = "environment-name";

    private SdxTestUtil() {
    }

    public static SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName(ENVIRONMENT_NAME);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }
}
