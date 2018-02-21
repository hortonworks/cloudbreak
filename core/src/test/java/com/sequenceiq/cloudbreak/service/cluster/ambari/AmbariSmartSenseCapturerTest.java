package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.Matchers.anyInt;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.domain.Cluster;
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
    private AmbariSmartSenseCapturer underTest = new AmbariSmartSenseCapturer();

    @Test
    public void testCaptureWhenNoTriggerNeedsToBeDone() {
        Cluster cluster = TestUtil.cluster();
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(cluster.getBlueprint().getBlueprintText(), smartSenseSubscription)).thenReturn(false);

        underTest.capture(1, cluster.getBlueprint().getBlueprintText(), ambariClient);

        verify(ambariClient, times(0)).smartSenseCapture(anyInt());
    }

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndNoExceptionOccured() {
        Cluster cluster = TestUtil.cluster();
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenReturn(1);
        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(cluster.getBlueprint().getBlueprintText(), smartSenseSubscription)).thenReturn(true);

        underTest.capture(1, cluster.getBlueprint().getBlueprintText(), ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }

    @Test
    public void testCaptureWheTriggerNeedsToBeDoneAndExceptionOccured() {
        Cluster cluster = TestUtil.cluster();
        Optional<SmartSenseSubscription> smartSenseSubscription = Optional.empty();
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClient.smartSenseCapture(1)).thenThrow(new AmbariServiceException("failed"));
        when(smartSenseSubscriptionService.getDefault()).thenReturn(smartSenseSubscription);
        when(smartsenseConfigurationLocator.smartsenseConfigurable(cluster.getBlueprint().getBlueprintText(), smartSenseSubscription)).thenReturn(true);

        underTest.capture(1, cluster.getBlueprint().getBlueprintText(), ambariClient);

        verify(ambariClient, times(1)).smartSenseCapture(anyInt());
    }
}