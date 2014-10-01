package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsStackDescription;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

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
    private MetaDataConverter metaDataConverter;

    private Stack stack;

    private StackJson stackJson;

    private StackDescription stackDescription;

    private AwsTemplate awsTemplate;

    private AwsCredential awsCredential;

    @Before
    public void setUp() {
        underTest = new StackConverter();
        MockitoAnnotations.initMocks(this);
        stackDescription = createStackDescription();
        awsCredential = new AwsCredential();
        awsCredential.setId(DUMMY_ID);
        awsTemplate = new AwsTemplate();
        awsTemplate.setId(DUMMY_ID);
        stack = createStack();
        stackJson = createStackJson();
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
        assertNotNull(result.getMetadata());
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
        assertNotNull(result.getMetadata());
        verify(clusterConverter, times(0)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackEntityToJsonWithStackDescription() {
        // GIVEN
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack, stackDescription);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getMetadata());
        verify(clusterConverter, times(1)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackEntityToJsonWithStackDescriptionWhenNoCluster() {
        // GIVEN
        stack.setCluster(null);
        given(metaDataConverter.convertAllJsonToEntity(anySetOf(InstanceMetaDataJson.class)))
                .willReturn(new HashSet<InstanceMetaData>());
        // WHEN
        StackJson result = underTest.convert(stack, stackDescription);
        // THEN
        assertEquals(result.getAmbariServerIp(), stack.getAmbariIp());
        assertEquals(result.getId(), stack.getId());
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertNotNull(result.getMetadata());
        verify(clusterConverter, times(0)).convert(any(Cluster.class), anyString());
    }

    @Test
    public void testConvertStackJsonToEntity() {
        // GIVEN
        given(credentialRepository.findOne(DUMMY_ID)).willReturn(awsCredential);
        given(templateRepository.findOne(DUMMY_ID)).willReturn(awsTemplate);
        // WHEN
        Stack result = underTest.convert(stackJson);
        // THEN
        assertEquals(result.getCredential().getId(), stackJson.getCredentialId());
        assertEquals(result.getTemplate().getId(), stackJson.getTemplateId());
        assertEquals(result.getStatus(), Status.REQUESTED);
        verify(credentialRepository, times(1)).findOne(anyLong());
        verify(templateRepository, times(1)).findOne(anyLong());
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertStackJsonToEntityWhenAccessDeniedOnCredential() {
        // GIVEN
        given(credentialRepository.findOne(DUMMY_ID)).willThrow(new AccessDeniedException(ACCESS_DENIED_MSG));
        // WHEN
        underTest.convert(stackJson);
    }

    @Test(expected = AccessDeniedException.class)
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
        stack.setStackCompleted(CF_STACK_COMPLETED);
        stack.setCluster(new Cluster());
        stack.setCredential(awsCredential);
        stack.setTemplate(awsTemplate);
        stack.setDescription(DESCRIPTION);
        stack.setHash(DUMMY_HASH);
        stack.setId(DUMMY_ID);
        stack.setInstanceMetaData(new HashSet<InstanceMetaData>());
        stack.setMetadataReady(METADATA_READY);
        stack.setName(DUMMY_NAME);
        stack.setNodeCount(NODE_COUNT);
        stack.setStatus(Status.AVAILABLE);
        stack.setStatusReason(DUMMY_STATUS_REASON);
        stack.setVersion(VERSION);
        stack.setPublicInAccount(true);
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
        stackJson.setMetadata(new HashSet<InstanceMetaDataJson>());
        stackJson.setName(DUMMY_NAME);
        stackJson.setNodeCount(NODE_COUNT);
        stackJson.setStatus(Status.AVAILABLE);
        stackJson.setTemplateId(DUMMY_ID);
        stackJson.setPublicInAccount(true);
        return stackJson;
    }

    private StackDescription createStackDescription() {
        DescribeStacksResult dSResult = new DescribeStacksResult();
        DescribeInstancesResult dIResult = new DescribeInstancesResult();
        return new AwsStackDescription(dSResult, dIResult);
    }

}
