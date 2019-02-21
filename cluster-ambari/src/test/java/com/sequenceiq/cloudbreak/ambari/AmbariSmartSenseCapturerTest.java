package com.sequenceiq.cloudbreak.ambari;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Ignore;
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
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@RunWith(MockitoJUnitRunner.class)
public class AmbariSmartSenseCapturerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @InjectMocks
    private final AmbariSmartSenseCapturer underTest = new AmbariSmartSenseCapturer();

    @Test
    @Ignore
    public void testCaptureWhenNoTriggerNeedsToBeDone() {
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)).thenReturn(false);

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(0)).smartSenseCapture(anyInt());
    }

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