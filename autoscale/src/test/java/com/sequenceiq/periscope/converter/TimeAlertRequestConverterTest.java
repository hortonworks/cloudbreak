package com.sequenceiq.periscope.converter;

import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.domain.TimeAlert;

@ExtendWith(MockitoExtension.class)
public class TimeAlertRequestConverterTest {

    @Mock
    ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @InjectMocks
    private TimeAlertRequestConverter underTest = new TimeAlertRequestConverter();

    @BeforeEach
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
        assertEquals(timeAlert.getName(), "timealert", "TimeAlert Name should match");
        assertEquals(timeAlert.getDescription(), "timealertdesc", "TimeAlert Desc should match");
        assertEquals(timeAlert.getCron(), "0   23  */2 *   *", "TimeAlert Cron should match");
        assertEquals(timeAlert.getTimeZone(), "GMT - 5", "TimeAlert Timezone should match");

        assertEquals(timeAlert.getScalingPolicy().getHostGroup(),
                "compute", "TimeAlert Hostgroup should match");
        assertEquals(timeAlert.getScalingPolicy().getAdjustmentType(), NODE_COUNT, "TimeAlert Adjustment Type should match");
        assertEquals(timeAlert.getScalingPolicy().getScalingAdjustment(), 10, "TimeAlert Adjustment should match");
    }

    private ScalingPolicyRequest getScalingPolicyRequest() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(NODE_COUNT);
        scalingPolicyRequest.setHostGroup("compute");
        scalingPolicyRequest.setScalingAdjustment(10);
        return scalingPolicyRequest;
    }
}
