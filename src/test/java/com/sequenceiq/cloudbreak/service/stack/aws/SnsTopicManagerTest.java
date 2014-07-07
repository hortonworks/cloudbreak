package com.sequenceiq.cloudbreak.service.stack.aws;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import reactor.core.Reactor;
import reactor.event.Event;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.credential.aws.CrossAccountCredentialsProvider;

public class SnsTopicManagerTest {

    public static final String DUMMY_TOKEN = "dummyToken";
    @InjectMocks
    @Spy
    private SnsTopicManager underTest = new SnsTopicManager();

    @Mock
    private CrossAccountCredentialsProvider credentialsProvider;

    @Mock
    private SnsTopicRepository snsTopicRepository;

    @Mock
    private Reactor reactor;

    @Mock
    private AmazonSNSClient snsClient;

    @Mock
    private StackRepository stackRepository;

    private AwsCredential credential;

    private CreateTopicResult createTopicResult;

    private ConfirmSubscriptionResult confirmSubscriptionResult;

    private SnsTopic snsTopic;

    private Stack stack;

    private User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        credential = AwsStackTestUtil.createAwsCredential();
        createTopicResult = new CreateTopicResult();
        createTopicResult.setTopicArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN);
        confirmSubscriptionResult = new ConfirmSubscriptionResult();
        confirmSubscriptionResult.setSubscriptionArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN);
        snsTopic = AwsStackTestUtil.createSnsTopic(AwsStackTestUtil.createAwsCredential());
        user = AwsStackTestUtil.createUser();
        stack = AwsStackTestUtil.createStack(user, credential, AwsStackTestUtil.createAwsTemplate(user));
    }

    @Test
    public void testCreateTopicAndSubscribe() {
        // GIVEN
        doReturn(snsClient).when(underTest).createSnsClient(credential, Regions.DEFAULT_REGION);
        given(snsClient.createTopic(anyString())).willReturn(createTopicResult);
        given(snsTopicRepository.save(any(SnsTopic.class))).willReturn(snsTopic);
        given(snsClient.subscribe(anyString(), anyString(), anyString())).willReturn(new SubscribeResult());
        // WHEN
        underTest.createTopicAndSubscribe(credential, Regions.DEFAULT_REGION);
        // THEN
        verify(snsClient, times(1)).subscribe(anyString(), anyString(), anyString());
    }

    @Test
    public void testConfirmSubscriptionWhenTopicIsNotConfirmedShouldNotifyAllRequestedStacks() {
        // GIVEN
        doReturn(snsClient).when(underTest).createSnsClient(any(AwsCredential.class), any(Regions.class));
        SnsRequest snsRequest = createSnsRequest();
        given(snsTopicRepository.findByTopicArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN)).willReturn(Arrays.asList(snsTopic, snsTopic));
        given(snsClient.confirmSubscription(anyString(), anyString())).willReturn(confirmSubscriptionResult);
        given(snsTopicRepository.save(any(SnsTopic.class))).willReturn(snsTopic);
        given(stackRepository.findRequestedStacksWithCredential(AwsStackTestUtil.DEFAULT_ID)).willReturn(Arrays.asList(stack, stack));
        // WHEN
        underTest.confirmSubscription(snsRequest);
        // THEN
        verify(reactor, times(2)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testConfirmSubscriptionWhenTopicIsAlreadyConfirmedShouldNotConfirmTopic() {
        // GIVEN
        SnsRequest snsRequest = createSnsRequest();
        snsTopic.setConfirmed(true);
        given(snsTopicRepository.findByTopicArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN)).willReturn(Arrays.asList(snsTopic));
        // WHEN
        underTest.confirmSubscription(snsRequest);
        // THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testConfirmSubscriptionWhenTopicsAreNotFoundShouldNotConfirmTopic() {
        // GIVEN
        SnsRequest snsRequest = createSnsRequest();
        given(snsTopicRepository.findByTopicArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN)).willReturn(new ArrayList<SnsTopic>());
        // WHEN
        underTest.confirmSubscription(snsRequest);
        // THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private SnsRequest createSnsRequest() {
        SnsRequest snsRequest = new SnsRequest();
        snsRequest.setTopicArn(AwsStackTestUtil.DEFAULT_TOPIC_ARN);
        snsRequest.setToken(DUMMY_TOKEN);
        return snsRequest;
    }
}
