package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class InitCodeGrantFlowHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialNotifier credentialNotifier;

    @Mock
    private Event<InitCodeGrantFlowRequest> initCodeGrantFlowRequestEvent;

    @Mock
    private InitCodeGrantFlowRequest data;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Platform platform;

    @Mock
    private Promise<InitCodeGrantFlowResponse> result;

    @Mock
    private CredentialConnector credentialConnector;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private InitCodeGrantFlowHandler underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(initCodeGrantFlowRequestEvent.getData()).thenReturn(data);
        when(data.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getPlatform()).thenReturn(platform);
        when(data.getResult()).thenReturn(result);
        when(cloudConnector.credentials()).thenReturn(credentialConnector);
        when(data.getCloudCredential()).thenReturn(cloudCredential);
    }

    @Test
    public void testAcceptWhenSomeRuntimeExceptionHappensThenOnNextCallStillHappens() {
        doThrow(new RuntimeException()).when(cloudPlatformConnectors).getDefault(platform);

        underTest.accept(initCodeGrantFlowRequestEvent);

        verify(cloudPlatformConnectors, times(1)).getDefault(platform);
        verify(data, times(1)).getResult();
        verify(result, times(1)).onNext(any(InitCodeGrantFlowResponse.class));
        verify(credentialConnector, times(0)).initCodeGrantFlow(cloudContext, cloudCredential);
    }

    @Test
    public void testAcceptWhenNoExceptionComesDuringExecutionThenOnNextCallHappens() {
        when(cloudPlatformConnectors.getDefault(platform)).thenReturn(cloudConnector);

        underTest.accept(initCodeGrantFlowRequestEvent);

        verify(cloudPlatformConnectors, times(1)).getDefault(platform);
        verify(data, times(1)).getResult();
        verify(result, times(1)).onNext(any(InitCodeGrantFlowResponse.class));
        verify(credentialConnector, times(1)).initCodeGrantFlow(cloudContext, cloudCredential);
    }

}