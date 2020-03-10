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

import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;

@RunWith(MockitoJUnitRunner.class)
public class TimeAlertResponseConverterTest {

    @Mock
    ScalingPolicyResponseConverter scalingPolicyResponseConverter;

    @InjectMocks
    private TimeAlertResponseConverter underTest = new TimeAlertResponseConverter();

    @Before
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
        assertEquals("TimeAlert Name should match", timeAlertResponse.getAlertName(), "timealert");
        assertEquals("TimeAlert Desc should match", timeAlertResponse.getDescription(), "timealertdesc");
        assertEquals("TimeAlert Cron should match", timeAlertResponse.getCron(), "0   23  */2 *   *");
        assertEquals("TimeAlert Timezone should match", timeAlertResponse.getTimeZone(), "GMT");

        assertEquals("TimeAlert Hostgroup should match",
                timeAlertResponse.getScalingPolicy().getHostGroup(), "compute");
        assertEquals("TimeAlert Adjustment Type should match",
                timeAlertResponse.getScalingPolicy().getAdjustmentType(), NODE_COUNT);
        assertEquals("TimeAlert Adjustment should match",
                timeAlertResponse.getScalingPolicy().getScalingAdjustment(), 10);
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
