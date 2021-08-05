package com.sequenceiq.it.cloudbreak.dto.telemetry;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class TelemetryTestDto extends AbstractCloudbreakTestDto<TelemetryRequest, TelemetryResponse, TelemetryTestDto> {

    private static final String TELEMETRY = "TELEMETRY";

    public TelemetryTestDto(TestContext testContext) {
        super(new TelemetryRequest(), testContext);
    }

    public TelemetryTestDto() {
        super(TELEMETRY);
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
        featuresRequest.addClusterLogsCollection(true);
        getRequest().setFeatures(featuresRequest);
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("dbusIncludeSaltLogs", true);
        fluentAttributes.put("dbusClusterLogsCollectionDisableStop", true);
        getRequest().setFluentAttributes(fluentAttributes);
        return this;
    }

    public TelemetryTestDto withOnlyCloudStorageLogging() {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addClusterLogsCollection(false);
        featuresRequest.addWorkloadAnalytics(false);
        featuresRequest.addCloudStorageLogging(true);
        getRequest().setFeatures(featuresRequest);
        return this;
    }
}
