package com.sequenceiq.it.cloudbreak.dto.telemetry;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
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

    public TelemetryTestDto withLogging() {
        LoggingRequest loggingRequest = new LoggingRequest();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(getCloudProvider().getInstanceProfile());
        loggingRequest.setS3(s3CloudStorageV1Parameters);
        loggingRequest.setStorageLocation(getCloudProvider().getBaseLocation());
        getRequest().setLogging(loggingRequest);
        return this;
    }
}
