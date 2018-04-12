package com.sequenceiq.cloudbreak.util;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Cluster;
import org.junit.Test;

import java.time.Duration;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StackUtilTest {
    private StackUtil stackUtil = new StackUtil();

    @Test
    public void testGetUptimeForClusterZero() {
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.CREATE_IN_PROGRESS);
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertEquals(0L, uptime);
    }

    @Test
    public void testGetUptimeForClusterNoGetUpSince() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        long uptime = stackUtil.getUptimeForCluster(cluster, false);
        assertEquals(Duration.ofMinutes(minutes).toMillis(), uptime);
    }

    @Test
    public void testGetUptimeForCluster() {
        Cluster cluster = new Cluster();
        int minutes = 10;
        cluster.setUptime(Duration.ofMinutes(minutes).toString());
        cluster.setStatus(Status.AVAILABLE);
        cluster.setUpSince(new Date().getTime());
        long uptime = stackUtil.getUptimeForCluster(cluster, true);
        assertTrue(uptime >= Duration.ofMinutes(minutes).toMillis());
    }

}