package com.sequenceiq.periscope.converter;

import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
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

import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.domain.LoadAlert;

@ExtendWith(MockitoExtension.class)
public class LoadAlertRequestConverterTest {

    @Mock
    ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @InjectMocks
    private LoadAlertRequestConverter underTest = new LoadAlertRequestConverter();

    @BeforeEach
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
        assertEquals(loadAlert.getName(), "loadalert", "LoadAlert Name should match");
        assertEquals(loadAlert.getDescription(), "loadalertdesc", "LoadAlert Desc should match");
        assertEquals(loadAlert.getScalingPolicy().getHostGroup(),
                "compute", "LoadAlert Hostgroup should match");
        assertEquals(loadAlert.getScalingPolicy().getAdjustmentType(),
                LOAD_BASED, "LoadAlert Adjustment Type should match");
        assertEquals(loadAlert.getScalingPolicy().getScalingAdjustment(),
                10, "LoadAlert Adjustment should match");
    }

    private ScalingPolicyRequest getScalingPolicyRequest() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(LOAD_BASED);
        scalingPolicyRequest.setHostGroup("compute");
        scalingPolicyRequest.setScalingAdjustment(10);
        return scalingPolicyRequest;
    }
}
