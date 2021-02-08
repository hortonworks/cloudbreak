package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.common.api.type.ResourceType;

public class StackUtilTest {

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @InjectMocks
    private final StackUtil stackUtil = new StackUtil();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

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

    @Test
    public void testCreateInstanceToVolumeInfoMapWhenEveryVolumeSetAreAttachedToInstance() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId"));
        volumeSets.add(getVolumeSetResource("secInstanceId"));
        volumeSets.add(getVolumeSetResource("thirdInstanceId"));

        Map<String, Map<String, Object>> actual = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        assertEquals(volumeSets.size(), actual.size());
    }

    @Test
    public void testCreateInstanceToVolumeInfoMapWhenNotEveryVolumeSetAreAttachedToInstance() {
        List<Resource> volumeSets = new ArrayList<>();
        volumeSets.add(getVolumeSetResource("anInstanceId"));
        volumeSets.add(getVolumeSetResource("secInstanceId"));
        volumeSets.add(getVolumeSetResource("thirdInstanceId"));
        volumeSets.add(getVolumeSetResource(null));
        volumeSets.add(getVolumeSetResource(null));

        Map<String, Map<String, Object>> actual = stackUtil.createInstanceToVolumeInfoMap(volumeSets);

        int numberOfVolumeSetsWithoutInstanceReference = 2;
        assertEquals(volumeSets.size() - numberOfVolumeSetsWithoutInstanceReference, actual.size());
    }

    private Resource getVolumeSetResource(String instanceID) {
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AZURE_VOLUMESET);
        resource.setInstanceId(instanceID);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes.Builder()
                .build();
        resource.setAttributes(new Json(volumeSetAttributes));
        return resource;
    }
}
