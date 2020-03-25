package com.sequenceiq.periscope.converter;

import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.domain.LoadAlert;

@RunWith(MockitoJUnitRunner.class)
public class LoadAlertRequestConverterTest {

    @Mock
    ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @InjectMocks
    private LoadAlertRequestConverter underTest = new LoadAlertRequestConverter();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertLoadAlertRequestToLoadAlert() {
        LoadAlertRequest req = new LoadAlertRequest();
        req.setAlertName("loadalert");
        req.setDescription("loadalertdesc");

        ScalingPolicyRequest scalingPolicyRequest = getScalingPolicyRequest();
        req.setScalingPolicy(scalingPolicyRequest);

        when(scalingPolicyRequestConverter.convert(any(ScalingPolicyRequest.class))).thenCallRealMethod();

        LoadAlert loadAlert = underTest.convert(req);
        assertEquals("LoadAlert Name should match", loadAlert.getName(), "loadalert");
        assertEquals("LoadAlert Desc should match", loadAlert.getDescription(), "loadalertdesc");
        assertEquals("LoadAlert Hostgroup should match",
                loadAlert.getScalingPolicy().getHostGroup(), "compute");
        assertEquals("LoadAlert Adjustment Type should match",
                loadAlert.getScalingPolicy().getAdjustmentType(), LOAD_BASED);
        assertEquals("LoadAlert Adjustment should match",
                loadAlert.getScalingPolicy().getScalingAdjustment(), 10);
    }

    private ScalingPolicyRequest getScalingPolicyRequest() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(LOAD_BASED);
        scalingPolicyRequest.setHostGroup("compute");
        scalingPolicyRequest.setScalingAdjustment(10);
        return scalingPolicyRequest;
    }
}
