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

import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;

@ExtendWith(MockitoExtension.class)
public class TimeAlertResponseConverterTest {

    @Mock
    ScalingPolicyResponseConverter scalingPolicyResponseConverter;

    @InjectMocks
    private TimeAlertResponseConverter underTest = new TimeAlertResponseConverter();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertTimeAlertToTimeAlertResponse() {
        TimeAlert timeAlert = new TimeAlert();
        timeAlert.setName("timealert");
        timeAlert.setDescription("timealertdesc");
        timeAlert.setCron("0   23  */2 *   *");
        timeAlert.setTimeZone("GMT");
        timeAlert.setId(20L);

        ScalingPolicy scalingPolicy = getScalingPolicy();
        timeAlert.setScalingPolicy(scalingPolicy);

        when(scalingPolicyResponseConverter.convert(any(ScalingPolicy.class))).thenCallRealMethod();

        TimeAlertResponse timeAlertResponse = underTest.convert(timeAlert);
        assertEquals(timeAlertResponse.getAlertName(), "timealert", "TimeAlert Name should match");
        assertEquals(timeAlertResponse.getDescription(), "timealertdesc", "TimeAlert Desc should match");
        assertEquals(timeAlertResponse.getCron(), "0   23  */2 *   *", "TimeAlert Cron should match");
        assertEquals(timeAlertResponse.getTimeZone(), "GMT", "TimeAlert Timezone should match");

        assertEquals(timeAlertResponse.getScalingPolicy().getHostGroup(), "compute", "TimeAlert Hostgroup should match");
        assertEquals(timeAlertResponse.getScalingPolicy().getAdjustmentType(), NODE_COUNT, "TimeAlert Adjustment Type should match");
        assertEquals(timeAlertResponse.getScalingPolicy().getScalingAdjustment(), 10, "TimeAlert Adjustment should match");
    }

    private ScalingPolicy getScalingPolicy() {
        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setName("loadalertpolicy");
        scalingPolicy.setAdjustmentType(NODE_COUNT);
        scalingPolicy.setHostGroup("compute");
        scalingPolicy.setScalingAdjustment(10);
        return scalingPolicy;
    }
}
