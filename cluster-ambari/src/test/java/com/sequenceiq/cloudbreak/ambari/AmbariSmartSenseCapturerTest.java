package com.sequenceiq.cloudbreak.ambari;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.clusterdefinition.SmartsenseConfigurationLocator;

@RunWith(MockitoJUnitRunner.class)
public class AmbariSmartSenseCapturerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @InjectMocks
    private final AmbariSmartSenseCapturer underTest = new AmbariSmartSenseCapturer();

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndNoExceptionOccured() {
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenReturn(1);

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndExceptionOccured() {
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenThrow(new AmbariServiceException("failed"));

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }
}