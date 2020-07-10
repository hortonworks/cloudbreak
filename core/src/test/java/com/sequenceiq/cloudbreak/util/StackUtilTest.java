package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.UptimeStat;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;

public class StackUtilTest {

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @InjectMocks
    private final StackUtil stackUtil = new StackUtil();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetUptimeForClusterZero() {
        long uptime = stackUtil.getUptimeForCluster(new UptimeStat(), true);
        assertEquals(0L, uptime);
    }

    @Test
    public void testGetUptimeForClusterNoGetUpSince() {
        UptimeStat uptimeStat = new UptimeStat();
        int minutes = 10;
        uptimeStat.setUptime(Duration.ofMinutes(minutes).toString());
        long uptime = stackUtil.getUptimeForCluster(uptimeStat, false);
        assertEquals(Duration.ofMinutes(minutes).toMillis(), uptime);
    }

    @Test
    public void testGetUptimeForCluster() {
        UptimeStat uptimeStat = new UptimeStat();
        int minutes = 10;
        uptimeStat.setUptime(Duration.ofMinutes(minutes).toString());
        uptimeStat.setUpSince(new Date().getTime());
        long uptime = stackUtil.getUptimeForCluster(uptimeStat, true);
        assertTrue(uptime >= Duration.ofMinutes(minutes).toMillis());
    }

    @Test
    public void testGetCloudCredential() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("envCrn");
        CloudCredential cloudCredential = new CloudCredential("123", "CloudCred");

        when(credentialClientService.getByEnvironmentCrn(anyString())).thenReturn(Credential.builder().build());
        when(credentialToCloudCredentialConverter.convert(any(Credential.class))).thenReturn(cloudCredential);

        CloudCredential result = stackUtil.getCloudCredential(stack);
        assertEquals(result.getId(), cloudCredential.getId());
        assertEquals(result.getName(), cloudCredential.getName());
    }
}
