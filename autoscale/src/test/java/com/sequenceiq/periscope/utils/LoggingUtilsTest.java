package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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

        assertEquals("ResourceCrn should match", AUTOSCALE_RESOURCE_CRN,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals("ResourceName should match", AUTOSCALE_RESOURCE_NAME,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals("Tenant should match", AUTOSCALE_CLUSTER_TENANT,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.TENANT.toString()));
        assertEquals("UserCrn should match", AUTOSCALE_USER_CRN,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.USER_CRN.toString()));
        assertEquals("Resource Type should match", "AutoscaleCluster",
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()));
    }

    @Test
    public void testBuildMdcContextWithName() {
        LoggingUtils.buildMdcContextWithName(AUTOSCALE_RESOURCE_NAME);

        assertEquals("ResourceName should match", AUTOSCALE_RESOURCE_NAME,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals("Resource Type should match", "AutoscaleCluster",
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()));
    }

    @Test
    public void testBuildMdcContextWithCrn() {
        LoggingUtils.buildMdcContextWithCrn(AUTOSCALE_RESOURCE_CRN);

        assertEquals("ResourceCrn should match", AUTOSCALE_RESOURCE_CRN,
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals("Resource Type should match", "AutoscaleCluster",
                MDCBuilder.getMdcContextMap().get(LoggerContextKey.RESOURCE_TYPE.toString()));
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
