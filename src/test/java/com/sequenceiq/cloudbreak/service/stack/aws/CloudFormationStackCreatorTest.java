package com.sequenceiq.cloudbreak.service.stack.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.domain.Status;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.Reactor;
import reactor.event.Event;

import java.util.Arrays;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

public class CloudFormationStackCreatorTest {

    private static final String STACK_ID = "stackId";

    @InjectMocks
    @Spy
    private CloudFormationStackCreator underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private CloudFormationTemplate cfTemplate;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private WebsocketService websocketService;

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

    private SnsTopic snsTopic;

    private AwsCredential credential;

    @Before
    public void setUp() {
        underTest = new CloudFormationStackCreator();
        MockitoAnnotations.initMocks(this);
        user = AwsStackTestUil.createUser();
        credential = AwsStackTestUil.createAwsCredential();
        stack = AwsStackTestUil.createStack(user, credential, AwsStackTestUil.createAwsTemplate(user));
        snsTopic = AwsStackTestUil.createSnsTopic(credential);

    }

    @Test
    public void testCreateCloudFormationStack() {
        //GIVEN
        given(stackRepository.findById(stack.getId())).willReturn(stack);
        given(stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS)).willReturn(stack);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        given(awsStackUtil.createCloudFormationClient(Regions.DEFAULT_REGION, credential)).willReturn(client);
        given(client.createStack(any(CreateStackRequest.class))).willReturn(createStackResult);
        given(createStackResult.getStackId()).willReturn(STACK_ID);
        given(stackUpdater.updateStackCfAttributes(stack.getId(),
                String.format("%s-%s", stack.getName(), stack.getId()), STACK_ID)).willReturn(stack);
        //WHEN
        underTest.createCloudFormationStack(stack, credential, snsTopic);
        //THEN
        verify(client, times(1)).createStack(any(CreateStackRequest.class));
    }

    @Test
    public void testCreateCloudFormationStackWhenStatusNotRequestedShouldNotCreateStack() {
        //GIVEN
        stack.setStatus(Status.CREATE_IN_PROGRESS);
        given(stackRepository.findById(stack.getId())).willReturn(stack);
        //WHEN
        underTest.createCloudFormationStack(stack, credential, snsTopic);
        //THEN
        verify(client, times(0)).createStack(any(CreateStackRequest.class));
    }

    @Test
    public void testCreateCloudFormationStackWhenRetrieveStackShouldThrowExceptionAndNotify() {
        //GIVEN
        given(stackRepository.findById(stack.getId())).willThrow(new NullPointerException());
        //WHEN
        underTest.createCloudFormationStack(stack, credential, snsTopic);
        //THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testStartAllRequestedStackCreationForTopic() {
        //GIVEN
        given(stackRepository.findRequestedStacksWithCredential(AwsStackTestUil.DEFAULT_ID)).willReturn(Arrays.asList(stack, stack));
        doNothing().when(underTest).createCloudFormationStack(stack, credential, snsTopic);
        //WHEN
        underTest.startAllRequestedStackCreationForTopic(snsTopic);
        //THEN
        verify(underTest, times(2)).createCloudFormationStack(stack, credential, snsTopic);
    }
}
