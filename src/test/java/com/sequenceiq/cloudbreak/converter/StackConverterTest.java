package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.network.SecurityService;

public class StackConverterTest {

    private static final String AMBARI_IP = "172.17.0.1";
    private static final String DUMMY_STACK_ID = "dummyStackId";
    private static final String DUMMY_STACK_NAME = "dummyStackName";
    private static final String DESCRIPTION = "description ...";
    private static final String DUMMY_HASH = "dummyHash";
    private static final String DUMMY_NAME = "dummyName";
    private static final String DUMMY_STATUS_REASON = "dummyStatusReason";
    private static final long VERSION = 1L;
    private static final int NODE_COUNT = 1;
    private static final long DUMMY_ID = 1L;
    private static final boolean CF_STACK_COMPLETED = false;
    private static final boolean METADATA_READY = true;
    private static final String ACCESS_DENIED_MSG = "access denied";

    @InjectMocks
    private StackConverter underTest;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private ClusterConverter clusterConverter;

    @Mock
    private InstanceGroupConverter instanceGroupConverter;

    @Mock
    private MetaDataConverter metaDataConverter;

    @Mock
    private SubnetConverter subnetConverter;

    @Mock
    private SecurityService securityService;

    private Stack stack;

    private StackJson stackJson;

    private AwsTemplate awsTemplate;

    private AwsCredential awsCredential;

    @Before
    public void setUp() {
        underTest = new StackConverter();
        MockitoAnnotations.initMocks(this);
        awsCredential = new AwsCredential();
        awsCredential.setId(DUMMY_ID);
        awsTemplate = new AwsTemplate();
        awsTemplate.setId(DUMMY_ID);
        stack = createStack();
        stackJson = createStackJson();
        given(instanceGroupConverter.convertAllJsonToEntity(anySetOf(InstanceGroupJson.class)))
                .willReturn(new HashSet<InstanceGroup>());
        given(instanceGroupConverter.convertAllEntityToJson(anySetOf(InstanceGroup.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
    }

    @Test
    public void testConvertStackEntityToJson() {
        // GIVEN
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getInstanceGroups());
        assertEquals(result.getAccount(), DUMMY_NAME);
        assertEquals(result.getOwner(), DUMMY_NAME);
        assertTrue(result.isPublicInAccount());
        verify(clusterConverter, times(1)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackEntityToJsonWhenNoCluster() {
        // GIVEN
        stack.setCluster(null);
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getInstanceGroups());
        verify(clusterConverter, times(0)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackEntityToJsonWithStackDescription() {
        // GIVEN
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getInstanceGroups());
        verify(clusterConverter, times(1)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackEntityToJsonWithStackDescriptionWhenNoCluster() {
        // GIVEN
        stack.setCluster(null);
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getInstanceGroups());
        verify(clusterConverter, times(0)).convert(any(Cluster.class), anyString());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertStackJsonToEntity() {
        // GIVEN
        given(credentialRepository.findOne(DUMMY_ID)).willReturn(awsCredential);
        given(templateRepository.findOne(DUMMY_ID)).willReturn(awsTemplate);
        // WHEN
        Stack result = underTest.convert(stackJson);
        // THEN
        assertEquals(result.getCredential().getId(), stackJson.getCredentialId());
        assertEquals(result.getStatus(), Status.REQUESTED);
        assertNull(result.getAccount());
        assertNull(result.getOwner());
        assertFalse(result.isPublicInAccount());
        verify(credentialRepository, times(1)).findOne(anyLong());
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertStackJsonToEntityWhenAccessDeniedOnCredential() {
        // GIVEN
        given(credentialRepository.findOne(DUMMY_ID)).willThrow(new AccessDeniedException(ACCESS_DENIED_MSG));
        // WHEN
        underTest.convert(stackJson);
    }

    public void testConvertStackJsonToEntityWhenAccessDeniedOnTemplate() {
        // GIVEN
        given(credentialRepository.findOne(DUMMY_ID)).willReturn(awsCredential);
        given(templateRepository.findOne(DUMMY_ID)).willThrow(new AccessDeniedException(ACCESS_DENIED_MSG));
        // WHEN
        underTest.convert(stackJson);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setAmbariIp(AMBARI_IP);
        stack.setCluster(new Cluster());
        stack.setCredential(awsCredential);
        stack.setDescription(DESCRIPTION);
        stack.setHash(DUMMY_HASH);
        stack.setId(DUMMY_ID);
        stack.setInstanceGroups(new HashSet<InstanceGroup>());
        stack.setMetadataReady(METADATA_READY);
        stack.setName(DUMMY_NAME);
        stack.setStatus(Status.AVAILABLE);
        stack.setStatusReason(DUMMY_STATUS_REASON);
        stack.setVersion(VERSION);
        stack.setPublicInAccount(true);
        stack.setAccount(DUMMY_NAME);
        stack.setOwner(DUMMY_NAME);
        stack.setAllowedSubnets(new HashSet<Subnet>());
        stack.setInstanceGroups(new HashSet<InstanceGroup>());
        return stack;
    }

    private StackJson createStackJson() {
        StackJson stackJson = new StackJson();
        stackJson.setId(DUMMY_ID);
        stackJson.setAmbariServerIp(AMBARI_IP);
        stackJson.setCloudPlatform(CloudPlatform.AWS);
        stackJson.setCluster(new ClusterResponse());
        stackJson.setCredentialId(DUMMY_ID);
        stackJson.setHash(DUMMY_HASH);
        stackJson.setPublicInAccount(false);
        stackJson.setAccount(DUMMY_HASH);
        stackJson.setOwner(DUMMY_HASH);
        stackJson.setInstanceGroups(new ArrayList<InstanceGroupJson>());
        stackJson.setName(DUMMY_NAME);
        stackJson.setStatus(Status.AVAILABLE);
        stackJson.setInstanceGroups(new ArrayList<InstanceGroupJson>());
        return stackJson;
    }

}
