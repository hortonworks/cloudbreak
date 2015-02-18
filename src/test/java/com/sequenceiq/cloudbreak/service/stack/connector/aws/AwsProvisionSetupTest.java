package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

import reactor.core.Reactor;
import reactor.event.Event;

public class AwsProvisionSetupTest {

    @InjectMocks
    private AwsProvisionSetup underTest;

    @Mock
    private Reactor reactor;

    private Template awsTemplate;

    private Stack stack;

    private Credential awsCredential;

    @Before
    public void setUp() {
        underTest = new AwsProvisionSetup();
        MockitoAnnotations.initMocks(this);
        awsCredential = ServiceTestUtils.createCredential(CloudPlatform.AWS);
        awsTemplate = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        stack = ServiceTestUtils.createStack(awsTemplate, awsCredential);
    }

    @Test
    public void testSetupProvisioningShouldCreateProvisionSetupCompleteReactorEvent() {
        // GIVEN
        // WHEN
        underTest.setupProvisioning(stack);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }
}
