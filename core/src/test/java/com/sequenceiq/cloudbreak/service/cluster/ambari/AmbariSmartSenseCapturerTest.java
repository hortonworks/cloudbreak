package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariSmartSenseCapturerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Mock
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @InjectMocks
    private final AmbariSmartSenseCapturer underTest = new AmbariSmartSenseCapturer();

    @Test
    public void testCaptureWhenNoTriggerNeedsToBeDone() {
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)).thenReturn(false);

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(0)).smartSenseCapture(anyInt());
    }

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndNoExceptionOccured() {
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenReturn(1);
        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)).thenReturn(true);

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndExceptionOccured() {
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenThrow(new AmbariServiceException("failed"));
        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)).thenReturn(true);

        underTest.capture(1, ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }
}