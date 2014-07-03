package com.sequenceiq.cloudbreak.service.stack.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.service.credential.aws.CrossAccountCredentialsProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

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
    private CloudFormationStackCreator cloudFormationStackCreator;

    @Mock
    private AmazonSNSClient snsClient;

    private AwsCredential credential;

    private CreateTopicResult createTopicResult;

    private ConfirmSubscriptionResult confirmSubscriptionResult;

    private SnsTopic snsTopic;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        credential = AwsStackTestUil.createAwsCredential();
        createTopicResult = new CreateTopicResult();
        createTopicResult.setTopicArn(AwsStackTestUil.DEFAULT_TOPIC_ARN);
        confirmSubscriptionResult = new ConfirmSubscriptionResult();
        confirmSubscriptionResult.setSubscriptionArn(AwsStackTestUil.DEFAULT_TOPIC_ARN);
        snsTopic = AwsStackTestUil.createSnsTopic(AwsStackTestUil.createAwsCredential());
    }

    @Test
    public void testCreateTopicAndSubscribe() {
        //GIVEN
        doReturn(snsClient).when(underTest).createSnsClient(credential, Regions.DEFAULT_REGION);
        given(snsClient.createTopic(anyString())).willReturn(createTopicResult);
        given(snsTopicRepository.save(any(SnsTopic.class))).willReturn(snsTopic);
        given(snsClient.subscribe(anyString(), anyString(), anyString())).willReturn(new SubscribeResult());
        //WHEN
        underTest.createTopicAndSubscribe(credential, Regions.DEFAULT_REGION);
        //THEN
        verify(snsClient, times(1)).subscribe(anyString(), anyString(), anyString());
    }

    @Test
    public void testConfirmSubscriptionWhenTopicIsNotConfirmedShouldConfirmOnlyOnce() {
        //GIVEN
        doReturn(snsClient).when(underTest).createSnsClient(any(AwsCredential.class), any(Regions.class));
        SnsRequest snsRequest = createSnsRequest();
        given(snsTopicRepository.findByTopicArn(AwsStackTestUil.DEFAULT_TOPIC_ARN)).willReturn(Arrays.asList(snsTopic, snsTopic));
        given(snsClient.confirmSubscription(anyString(), anyString())).willReturn(confirmSubscriptionResult);
        given(snsTopicRepository.save(any(SnsTopic.class))).willReturn(snsTopic);
        doNothing().when(cloudFormationStackCreator).startAllRequestedStackCreationForTopic(snsTopic);
        //WHEN
        underTest.confirmSubscription(snsRequest);
        //THEN
        verify(cloudFormationStackCreator, times(1)).startAllRequestedStackCreationForTopic(snsTopic);
    }

    @Test
    public void testConfirmSubscriptionWhenTopicIsAlreadyConfirmedShouldNotConfirmTopic() {
        //GIVEN
        SnsRequest snsRequest = createSnsRequest();
        snsTopic.setConfirmed(true);
        given(snsTopicRepository.findByTopicArn(AwsStackTestUil.DEFAULT_TOPIC_ARN)).willReturn(Arrays.asList(snsTopic));
        //WHEN
        underTest.confirmSubscription(snsRequest);
        //THEN
        verify(cloudFormationStackCreator, times(0)).startAllRequestedStackCreationForTopic(snsTopic);
    }

    @Test
    public void testConfirmSubscriptionWhenTopicsAreNotFoundShouldNotConfirmTopic() {
        //GIVEN
        SnsRequest snsRequest = createSnsRequest();
        given(snsTopicRepository.findByTopicArn(AwsStackTestUil.DEFAULT_TOPIC_ARN)).willReturn(new ArrayList<SnsTopic>());
        //WHEN
        underTest.confirmSubscription(snsRequest);
        //THEN
        verify(cloudFormationStackCreator, times(0)).startAllRequestedStackCreationForTopic(snsTopic);
    }

    private SnsRequest createSnsRequest() {
        SnsRequest snsRequest = new SnsRequest();
        snsRequest.setTopicArn(AwsStackTestUil.DEFAULT_TOPIC_ARN);
        snsRequest.setToken(DUMMY_TOKEN);
        return snsRequest;
    }
}
