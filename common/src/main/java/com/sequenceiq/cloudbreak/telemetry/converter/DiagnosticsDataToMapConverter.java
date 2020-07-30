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

    public Map<String, Object> convert(BaseDiagnosticsCollectionRequest request, Telemetry telemetry, String region) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", request.getDestination().toString());
        parameters.put("issue", request.getIssue());
        parameters.put("description", request.getDescription());
        parameters.put("labelFilter", request.getLabels());
        parameters.put("startTime", Optional.ofNullable(request.getStartTime())
                .map(Date::getTime).orElse(null));
        parameters.put("endTime", Optional.ofNullable(request.getEndTime())
                .map(Date::getTime).orElse(null));
        parameters.put("hostGroups", Optional.ofNullable(request.getHostGroups()).orElse(null));
        parameters.put("hosts", Optional.ofNullable(request.getHosts()).orElse(null));
        parameters.put("includeSaltLogs", Optional.ofNullable(request.getIncludeSaltLogs()).orElse(false));
        parameters.put("updatePackage", Optional.ofNullable(request.getUpdatePackage()).orElse(false));
        if (telemetry != null && telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            if (logging.getS3() != null) {
                S3Config s3Config = s3ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
                parameters.put("s3_bucket", s3Config.getBucket());
                parameters.put("s3_location", Paths.get(s3Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
                parameters.put("s3_region", region);
            }
            if (logging.getAdlsGen2() != null) {
                AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(logging.getStorageLocation());
                parameters.put("adlsv2_storage_account", adlsGen2Config.getAccount());
                parameters.put("adlsv2_storage_container", adlsGen2Config.getFileSystem());
                parameters.put("adlsv2_storage_location", Paths.get(adlsGen2Config.getFolderPrefix(), DIAGNOSTICS_SUFFIX_PATH).toString());
            }
        }
        parameters.put("additionalLogs", request.getAdditionalLogs());
        return Collections.singletonMap("filecollector", parameters);
    }
}
