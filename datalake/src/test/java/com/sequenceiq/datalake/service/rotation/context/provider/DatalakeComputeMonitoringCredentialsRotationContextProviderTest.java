package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.datalake.entity.SdxCluster;

@ExtendWith(MockitoExtension.class)
public class DatalakeComputeMonitoringCredentialsRotationContextProviderTest {

    @InjectMocks
    private DatalakeComputeMonitoringCredentialsRotationContextProvider underTest;

    @Test
    void testIsApplicable() throws JsonProcessingException {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        StackV4Request stackV4Request = new StackV4Request();
        TelemetryRequest telemetry = new TelemetryRequest();
        FeaturesRequest features = new FeaturesRequest();
        FeatureSetting monitoring = new FeatureSetting();
        monitoring.setEnabled(Boolean.TRUE);
        features.setMonitoring(monitoring);
        telemetry.setFeatures(features);
        stackV4Request.setTelemetry(telemetry);
        when(sdxCluster.getStackRequest()).thenReturn(JsonUtil.writeValueAsString(stackV4Request));
        assertTrue(underTest.isApplicable(sdxCluster));
    }
}
