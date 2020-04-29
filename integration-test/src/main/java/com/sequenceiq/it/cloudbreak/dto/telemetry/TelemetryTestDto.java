package com.sequenceiq.it.cloudbreak.dto.telemetry;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class TelemetryTestDto extends AbstractCloudbreakTestDto<TelemetryRequest, TelemetryResponse, TelemetryTestDto> {

    private static final String TELEMETRY = "TELEMETRY";

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private AzureCloudProvider azureCloudProvider;

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

        switch (getTestContext().getCloudProvider().getCloudPlatform()) {
            case AWS:
                S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
                s3CloudStorageV1Parameters.setInstanceProfile(awsCloudProvider.getInstanceProfile());
                loggingRequest.setS3(s3CloudStorageV1Parameters);
                loggingRequest.setStorageLocation(awsCloudProvider.getBaseLocation());
                getRequest().setLogging(loggingRequest);
                break;
            case AZURE:
                AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
                adlsGen2CloudStorageV1Parameters.setManagedIdentity(azureCloudProvider.getLoggerIdentity());
                adlsGen2CloudStorageV1Parameters.setSecure(azureCloudProvider.getSecure());
                loggingRequest.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
                loggingRequest.setStorageLocation(azureCloudProvider.getBaseLocation());
                getRequest().setLogging(loggingRequest);
                break;
            default:
                break;
        }
        return this;
    }

    public TelemetryTestDto withReportClusterLogs() {
        FeaturesRequest featuresRequest = new FeaturesRequest();
        featuresRequest.addClusterLogsCollection(true);
        getRequest().setFeatures(featuresRequest);
        Map<String, Object> fluentAttributes = new HashMap<>();
        fluentAttributes.put("dbusIncludeSaltLogs", true);
        getRequest().setFluentAttributes(fluentAttributes);
        return this;
    }
}
