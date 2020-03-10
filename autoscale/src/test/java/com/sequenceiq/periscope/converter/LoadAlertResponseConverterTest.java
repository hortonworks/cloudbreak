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

import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@RunWith(MockitoJUnitRunner.class)
public class LoadAlertResponseConverterTest {

    @Mock
    ScalingPolicyResponseConverter scalingPolicyResponseConverter;

    @InjectMocks
    private LoadAlertResponseConverter underTest = new LoadAlertResponseConverter();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertLoadAlertToLoadAlertResponse() {
        LoadAlert req = new LoadAlert();
        req.setName("loadalert");
        req.setDescription("loadalertdesc");
        req.setId(10L);

        ScalingPolicy scalingPolicy = getScalingPolicy();
        req.setScalingPolicy(scalingPolicy);

        when(scalingPolicyResponseConverter.convert(any(ScalingPolicy.class))).thenCallRealMethod();

        LoadAlertResponse loadAlertResponse = underTest.convert(req);
        assertEquals("LoadAlert Name should match", loadAlertResponse.getAlertName(), "loadalert");
        assertEquals("LoadAlert Desc should match", loadAlertResponse.getDescription(), "loadalertdesc");
        assertEquals("LoadAlert Hostgroup should match",
                loadAlertResponse.getScalingPolicy().getHostGroup(), "compute");
        assertEquals("LoadAlert Adjustment Type should match",
                loadAlertResponse.getScalingPolicy().getAdjustmentType(), NODE_COUNT);
        assertEquals("LoadAlert Adjustment should match",
                loadAlertResponse.getScalingPolicy().getScalingAdjustment(), 10);
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
