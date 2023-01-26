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

import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@ExtendWith(MockitoExtension.class)
public class LoadAlertResponseConverterTest {

    @Mock
    ScalingPolicyResponseConverter scalingPolicyResponseConverter;

    @InjectMocks
    private LoadAlertResponseConverter underTest = new LoadAlertResponseConverter();

    @BeforeEach
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
        assertEquals(loadAlertResponse.getAlertName(), "loadalert", "LoadAlert Name should match");
        assertEquals(loadAlertResponse.getDescription(), "loadalertdesc", "LoadAlert Desc should match");
        assertEquals(loadAlertResponse.getScalingPolicy().getHostGroup(),
                "compute", "LoadAlert Hostgroup should match");
        assertEquals(loadAlertResponse.getScalingPolicy().getAdjustmentType(), NODE_COUNT, "LoadAlert Adjustment Type should match");
        assertEquals(loadAlertResponse.getScalingPolicy().getScalingAdjustment(), 10, "LoadAlert Adjustment should match");
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
