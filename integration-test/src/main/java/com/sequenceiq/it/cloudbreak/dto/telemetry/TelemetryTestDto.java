package com.sequenceiq.it.cloudbreak.dto.telemetry;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.MonitoringRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class TelemetryTestDto extends AbstractCloudbreakTestDto<TelemetryRequest, TelemetryResponse, TelemetryTestDto> {

    public TelemetryTestDto(TestContext testContext) {
        super(new TelemetryRequest(), testContext);
    }

    public TelemetryTestDto valid() {
        return getCloudProvider().telemetry(this);
    }

    public TelemetryTestDto withLogging(CloudPlatform customCloudPlatform) {
        setCloudPlatform(customCloudPlatform);
        return withLogging();
    }

    public TelemetryTestDto withLogging() {
        getRequest().setLogging(getCloudProvider().loggingRequest(this));
        return this;
    }

    public TelemetryTestDto withReportClusterLogs() {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        getRequest().setFeatures(featuresRequest);
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("dbusIncludeSaltLogs", true);
        getRequest().setFluentAttributes(fluentAttributes);
        return this;
    }

    public TelemetryTestDto withReportClusterLogsWithoutWorkloadAnalytics() {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addWorkloadAnalytics(false);
        getRequest().setFeatures(featuresRequest);
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("dbusIncludeSaltLogs", true);
        getRequest().setFluentAttributes(fluentAttributes);
        return this;
    }

    public TelemetryTestDto withOnlyCloudStorageLogging() {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addWorkloadAnalytics(false);
        featuresRequest.addCloudStorageLogging(true);
        getRequest().setFeatures(featuresRequest);
        return this;
    }

    public TelemetryTestDto withMonitoring(String remoteWriteUrl) {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addMonitoring(true);
        getRequest().setFeatures(featuresRequest);

        MonitoringRequest monitoringRequest = new MonitoringRequest();
        monitoringRequest.setRemoteWriteUrl(remoteWriteUrl);
        getRequest().setMonitoring(monitoringRequest);
        return this;
    }
}
