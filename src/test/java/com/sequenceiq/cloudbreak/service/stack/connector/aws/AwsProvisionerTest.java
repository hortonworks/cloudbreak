package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

import reactor.core.Reactor;

public class AwsProvisionerTest {

    private static final String STACK_ID = "stackId";

    @InjectMocks
    @Spy
    private AwsConnector underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private CloudFormationTemplateBuilder cfTemplateBuilder;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private AmazonCloudFormationClient client;

    @Mock
    private CreateStackResult createStackResult;

    @Mock
    private Reactor reactor;

    private Stack stack;

    private Credential credential;

    @Before
    public void setUp() {
        underTest = new AwsConnector();
        MockitoAnnotations.initMocks(this);
        credential = ServiceTestUtils.createCredential(CloudPlatform.AWS);
        Set<Resource> resources = new HashSet<>();
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        stack = ServiceTestUtils.createStack(ServiceTestUtils.createTemplate(CloudPlatform.AWS), credential, resources);
    }

    @Test
    @Ignore
    public void testBuildStackCreateStackCalledAndContainsEveryRequiredParameter() {
        // GIVEN
        CreateStackRequest createStackRequest = new CreateStackRequest();
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(client);
        given(client.createStack(any(CreateStackRequest.class))).willReturn(createStackResult);
        given(createStackResult.getStackId()).willReturn(STACK_ID);
        given(stackUpdater.updateStackResources(anyLong(), anySet())).willReturn(stack);
        given(underTest.createStackRequest()).willReturn(createStackRequest);
        given(cfTemplateBuilder.build(anyString(), anyInt(), anyBoolean())).willReturn("templatebody");
        Map<String, Object> setupProperties = new HashMap<>();
        setupProperties.put(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY, "topicArn");
        // WHEN
        underTest.buildStack(stack, "test-userdata", setupProperties);
        // THEN
        assertEquals("topicArn", createStackRequest.getNotificationARNs().get(0));
        assertEquals("templatebody", createStackRequest.getTemplateBody());
        assertTrue(FluentIterable.from(createStackRequest.getParameters()).anyMatch(new Predicate<Parameter>() {
            @Override
            public boolean apply(Parameter parameter) {
                return "SSHLocation".equals(parameter.getParameterKey()) && AwsConnectorTestUtil.SSH_LOCATION.equals(parameter.getParameterValue());
            }
        }));
        assertTrue(FluentIterable.from(createStackRequest.getParameters()).anyMatch(new Predicate<Parameter>() {
            @Override
            public boolean apply(Parameter parameter) {
                return "CBUserData".equals(parameter.getParameterKey()) && "test-userdata".equals(parameter.getParameterValue());
            }
        }));
        assertTrue(FluentIterable.from(createStackRequest.getParameters()).anyMatch(new Predicate<Parameter>() {
            @Override
            public boolean apply(Parameter parameter) {
                return "StackName".equals(parameter.getParameterKey()) && (AwsConnectorTestUtil.STACK_NAME + "-1").equals(parameter.getParameterValue());
            }
        }));
        assertTrue(FluentIterable.from(createStackRequest.getParameters()).anyMatch(new Predicate<Parameter>() {
            @Override
            public boolean apply(Parameter parameter) {
                return "InstanceCount".equals(parameter.getParameterKey()) && AwsConnectorTestUtil.NODE_COUNT.toString().equals(parameter.getParameterValue());
            }
        }));
        verify(client, times(1)).createStack(createStackRequest);
        verify(stackUpdater, times(1)).updateStackResources(anyLong(), anySet());
    }
}
