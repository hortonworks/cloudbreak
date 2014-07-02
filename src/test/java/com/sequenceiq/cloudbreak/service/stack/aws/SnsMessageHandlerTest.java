package com.sequenceiq.cloudbreak.service.stack.aws;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.Reactor;
import reactor.event.Event;

import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

public class SnsMessageHandlerTest {

    private static final String MESSAGE_SUBJECT = "AWS CloudFormation Notification";
    private static final String RESOURCE_TYPE = "ResourceType";
    private static final String RESOURCE_STATUS = "ResourceStatus";

    @InjectMocks
    private SnsMessageHandler underTest;

    @Mock
    private SnsMessageParser snsMessageParser;

    @Mock
    private SnsTopicManager snsTopicManager;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private Reactor reactor;

    @Mock
    private AwsStackUtil awsStackUtil;

    private SnsRequest snsRequest;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new SnsMessageHandler();
        MockitoAnnotations.initMocks(this);
        snsRequest = createSnsRequest();
        User user = AwsStackTestUil.createUser();
        AwsCredential credential = AwsStackTestUil.createAwsCredential();
        AwsTemplate awsTemplate = AwsStackTestUil.createAwsTemplate(user);
        stack = AwsStackTestUil.createStack(user, credential, awsTemplate);
    }

    @Test
    public void testHandleMessageCreationComplete() {
        //GIVEN
        given(snsMessageParser.parseCFMessage(snsRequest.getMessage())).willReturn(createCFMessage());
        given(stackRepository.findByCfStackId(anyString())).willReturn(stack);
        given(stackUpdater.updateCfStackCreateComplete(anyLong())).willReturn(stack);
        //WHEN
        underTest.handleMessage(snsRequest);
        //THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testHandleMessageCreationCompleteNoStack() {
        //GIVEN
        given(snsMessageParser.parseCFMessage(snsRequest.getMessage())).willReturn(createCFMessage());
        given(stackRepository.findByCfStackId(anyString())).willReturn(null);
        //WHEN
        underTest.handleMessage(snsRequest);
        //THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testHandleMessageWhenStatusNotFailedAndCfNotCompletedShouldStackCreateFailed() {
        //GIVEN
        Map<String, String> cfMessage = createCFMessage();
        cfMessage.put(RESOURCE_TYPE, "AWS::CloudFormation::Stack");
        cfMessage.put(RESOURCE_STATUS, "ROLLBACK_IN_PROGRESS");
        stack.setStatus(Status.CREATE_IN_PROGRESS);
        given(snsMessageParser.parseCFMessage(snsRequest.getMessage())).willReturn(cfMessage);
        given(stackRepository.findByCfStackId(anyString())).willReturn(stack);
        //WHEN
        underTest.handleMessage(snsRequest);
        //THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testHandleMessageWhenStatusIsFailedShouldStackCreateAlreadyFailed() {
        //GIVEN
        Map<String, String> cfMessage = createCFMessage();
        cfMessage.put(RESOURCE_STATUS, "CREATE_FAILED");
        stack.setStatus(Status.CREATE_FAILED);
        given(snsMessageParser.parseCFMessage(snsRequest.getMessage())).willReturn(cfMessage);
        given(stackRepository.findByCfStackId(anyString())).willReturn(stack);
        //WHEN
        underTest.handleMessage(snsRequest);
        //THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testHandleMessageWhenStackNotFoundShouldStackCreationFailed() {
        //GIVEN
        Map<String, String> cfMessage = createCFMessage();
        cfMessage.put(RESOURCE_STATUS, "CREATE_FAILED");
        given(snsMessageParser.parseCFMessage(snsRequest.getMessage())).willReturn(cfMessage);
        given(stackRepository.findByCfStackId(anyString())).willReturn(null);
        //WHEN
        underTest.handleMessage(snsRequest);
        //THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private SnsRequest createSnsRequest() {
        SnsRequest req = new SnsRequest();
        req.setMessage("dummyMessage");
        req.setType("dummType");
        req.setSubject(MESSAGE_SUBJECT);
        return req;
    }

    private Map<String, String> createCFMessage() {
        Map<String, String> cfMessage = Maps.newHashMap();
        cfMessage.put(RESOURCE_TYPE, "AWS::CloudFormation::Stack");
        cfMessage.put(RESOURCE_STATUS, "CREATE_COMPLETE");
        return cfMessage;
    }
}
