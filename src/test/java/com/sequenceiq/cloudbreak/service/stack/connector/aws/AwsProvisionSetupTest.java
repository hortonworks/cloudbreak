package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

import reactor.core.Reactor;
import reactor.event.Event;

public class AwsProvisionSetupTest {

    @InjectMocks
    private AwsProvisionSetup underTest;

    @Mock
    private SnsTopicRepository snsTopicRepository;

    @Mock
    private SnsTopicManager snsTopicManager;

    @Mock
    private Reactor reactor;

    private Template awsTemplate;

    private Stack stack;

    private Credential awsCredential;

    private SnsTopic snsTopic;

    @Before
    public void setUp() {
        underTest = new AwsProvisionSetup();
        MockitoAnnotations.initMocks(this);
        awsCredential = ServiceTestUtils.createCredential(CloudPlatform.AWS);
        awsTemplate = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        stack = ServiceTestUtils.createStack(awsTemplate, awsCredential);
        snsTopic = ServiceTestUtils.createSnsTopic(awsCredential);
    }

    @Test
    public void testSetupProvisioningWhenSubscriptionIsNotConfirmed() {
        // GIVEN
        given(snsTopicRepository.findOneForCredentialInRegion(anyLong(), any(Regions.class))).willReturn(snsTopic);
        // WHEN
        underTest.setupProvisioning(stack);
        // THEN
        verify(snsTopicManager, times(1)).subscribeToTopic(any(AwsCredential.class), any(Regions.class), anyString());
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testSetupProvisioningWhenSubscriptionIsConfirmed() {
        // GIVEN
        snsTopic.setConfirmed(true);
        given(snsTopicRepository.findOneForCredentialInRegion(anyLong(), any(Regions.class))).willReturn(snsTopic);
        // WHEN
        underTest.setupProvisioning(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testSetupProvisioningWhenStackNotFoundForCredential() {
        // GIVEN
        // WHEN
        underTest.setupProvisioning(stack);
        // THEN
        verify(snsTopicManager, times(1)).createTopicAndSubscribe(any(AwsCredential.class), any(Regions.class));
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }
}
