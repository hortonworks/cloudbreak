package com.sequenceiq.cloudbreak.telemetry.converter;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.diagnostics.BaseDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class DiagnosticsDataToMapConverter {

    private static final String DIAGNOSTICS_SUFFIX_PATH = "diagnostics";

    @Inject
    private S3ConfigGenerator s3ConfigGenerator;

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public Map<String, Object> convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", request.getDestination().toString());
        parameters.put("issue", request.getIssue());
        parameters.put("description", request.getDescription());
        parameters.put("labelFilter", request.getLabels());
        parameters.put("startTime", Optional.ofNullable(request.getStartTime())
                .map(Date::getTime).orElse(null));
        parameters.put("endTime", Optional.ofNullable(request.getEndTime())
                .map(Date::getTime).orElse(null));
        if (telemetry != null && telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            if (logging.getS3() != null) {
                S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
                parameters.put("s3BaseUrl", "s3://" + Paths.get(s3Config.getBucket(), s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            }
            if (logging.getAdlsGen2() != null) {
                AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
                parameters.put("adlsV2BaseUrl", "https://" + Paths.get(String.format("%s%s", adlsGen2Config.getAccount(),
                        AdlsGen2ConfigGenerator.AZURE_DFS_DOMAIN_SUFFIX), adlsGen2Config.getFileSystem(),
                        adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
                parameters.put("azureInstanceMsi", logging.getAdlsGen2().getManagedIdentity());
            }
        }
        parameters.put("additionalLogs", request.getAdditionalLogs());
        return Collections.singletonMap("filecollector", parameters);
    }
}
