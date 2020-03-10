package com.sequenceiq.periscope.converter;

import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
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

import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.domain.TimeAlert;

@RunWith(MockitoJUnitRunner.class)
public class TimeAlertRequestConverterTest {

    @Mock
    ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @InjectMocks
    private TimeAlertRequestConverter underTest = new TimeAlertRequestConverter();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertTimeAlertRequestToTimeAlert() {
        TimeAlertRequest req = new TimeAlertRequest();
        req.setAlertName("timealert");
        req.setDescription("timealertdesc");
        req.setTimeZone("GMT - 5");
        req.setCron("0   23  */2 *   *");

        ScalingPolicyRequest scalingPolicyRequest = getScalingPolicyRequest();
        req.setScalingPolicy(scalingPolicyRequest);

        when(scalingPolicyRequestConverter.convert(any(ScalingPolicyRequest.class))).thenCallRealMethod();

        TimeAlert timeAlert = underTest.convert(req);
        assertEquals("TimeAlert Name should match", timeAlert.getName(), "timealert");
        assertEquals("TimeAlert Desc should match", timeAlert.getDescription(), "timealertdesc");
        assertEquals("TimeAlert Cron should match", timeAlert.getCron(), "0   23  */2 *   *");
        assertEquals("TimeAlert Timezone should match", timeAlert.getTimeZone(), "GMT - 5");

        assertEquals("TimeAlert Hostgroup should match",
                timeAlert.getScalingPolicy().getHostGroup(), "compute");
        assertEquals("TimeAlert Adjustment Type should match",
                timeAlert.getScalingPolicy().getAdjustmentType(), NODE_COUNT);
        assertEquals("TimeAlert Adjustment should match",
                timeAlert.getScalingPolicy().getScalingAdjustment(), 10);
    }

    private ScalingPolicyRequest getScalingPolicyRequest() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(NODE_COUNT);
        scalingPolicyRequest.setHostGroup("compute");
        scalingPolicyRequest.setScalingAdjustment(10);
        return scalingPolicyRequest;
    }
}
