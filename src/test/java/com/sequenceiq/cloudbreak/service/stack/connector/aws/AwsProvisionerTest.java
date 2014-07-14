package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import reactor.core.Reactor;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;

public class AwsProvisionerTest {

    private static final String STACK_ID = "stackId";

    @InjectMocks
    @Spy
    private AwsProvisioner underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private CloudFormationTemplate cfTemplate;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private AmazonCloudFormationClient client;

    @Mock
    private CreateStackResult createStackResult;

    @Mock
    private Reactor reactor;

    private Stack stack;

    private User user;

    private AwsCredential credential;

    @Before
    public void setUp() {
        underTest = new AwsProvisioner();
        MockitoAnnotations.initMocks(this);
        user = AwsConnectorTestUtil.createUser();
        credential = AwsConnectorTestUtil.createAwsCredential();
        stack = AwsConnectorTestUtil.createStack(user, credential, AwsConnectorTestUtil.createAwsTemplate(user));
    }

    @Test
    public void testBuildStackCreateStackCalledAndContainsEveryRequiredParameter() {
        // GIVEN
        CreateStackRequest createStackRequest = new CreateStackRequest();
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(client);
        given(client.createStack(any(CreateStackRequest.class))).willReturn(createStackResult);
        given(createStackResult.getStackId()).willReturn(STACK_ID);
        given(stackUpdater.updateStackCfAttributes(stack.getId(),
                String.format("%s-%s", stack.getName(), stack.getId()), STACK_ID)).willReturn(stack);
        given(underTest.createStackRequest()).willReturn(createStackRequest);
        given(cfTemplate.getBody()).willReturn("templatebody");
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
        verify(stackUpdater, times(1)).updateStackCfAttributes(stack.getId(),
                String.format("%s-%s", stack.getName(), stack.getId()), createStackResult.getStackId());
    }
}
