package com.sequenceiq.periscope.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;

public class LoggingUtilsTest {

    private static final String AUTOSCALE_RESOURCE_CRN = "someCrn";

    private static final String AUTOSCALE_RESOURCE_NAME = "someName";

    private static final String AUTOSCALE_CLUSTER_TENANT = "someTenant";

    private static final String AUTOSCALE_USER_CRN = "someUser";

    @Test
    public void testBuildMdcContext() {
        Cluster cluster = getACluster();

        LoggingUtils.buildMdcContext(cluster);

        assertEquals(AUTOSCALE_RESOURCE_CRN, MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_CRN.toString()),
                "ResourceCrn should match");
        assertEquals(AUTOSCALE_RESOURCE_NAME, MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_NAME.toString()),
                "ResourceName should match");
        assertEquals(AUTOSCALE_CLUSTER_TENANT, MDCBuilder.getMdcContextMap().get(LoggerContextKey.TENANT.toString()),
                "Tenant should match");
        assertEquals(AUTOSCALE_USER_CRN, MDCBuilder.getMdcContextMap().get(LoggerContextKey.USER_CRN.toString()),
                "UserCrn should match");
        assertEquals("AutoscaleCluster", MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()),
                "Resource Type should match");
    }

    @Test
    public void testBuildMdcContextWithName() {
        LoggingUtils.buildMdcContextWithName(AUTOSCALE_RESOURCE_NAME);

        assertEquals(AUTOSCALE_RESOURCE_NAME, MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_NAME.toString()),
                "ResourceName should match");
        assertEquals("AutoscaleCluster", MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()),
                "Resource Type should match");
    }

    @Test
    public void testBuildMdcContextWithCrn() {
        LoggingUtils.buildMdcContextWithCrn(AUTOSCALE_RESOURCE_CRN);

        assertEquals(AUTOSCALE_RESOURCE_CRN, MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_CRN.toString()),
                "ResourceCrn should match");
        assertEquals("AutoscaleCluster", MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()),
                "Resource Type should match");
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(AUTOSCALE_RESOURCE_CRN);
        cluster.setStackName(AUTOSCALE_RESOURCE_NAME);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(AUTOSCALE_CLUSTER_TENANT);
        clusterPertain.setUserCrn(AUTOSCALE_USER_CRN);
        cluster.setClusterPertain(clusterPertain);

        return cluster;
    }

}
