package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class CloudStorageFolderResolverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageFolderResolverService.class);

    private final S3ConfigGenerator s3ConfigGenerator;

    private final AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    private final GcsConfigGenerator gcsConfigGenerator;

    public CloudStorageFolderResolverService(S3ConfigGenerator s3ConfigGenerator,
            AdlsGen2ConfigGenerator adlsGen2ConfigGenerator,
            GcsConfigGenerator gcsConfigGenerator) {
        this.s3ConfigGenerator = s3ConfigGenerator;
        this.adlsGen2ConfigGenerator = adlsGen2ConfigGenerator;
        this.gcsConfigGenerator = gcsConfigGenerator;
    }

    public void updateStorageLocation(Telemetry telemetry, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Updating/enriching telemetry storage locations with cluster data.");
        if (telemetry != null && telemetry.getLogging() != null
                && StringUtils.isNotEmpty(telemetry.getLogging().getStorageLocation())) {
            Logging logging = telemetry.getLogging();
            String storageLocation = logging.getStorageLocation();
            if (logging.getS3() != null) {
                storageLocation = resolveS3Location(storageLocation,
                        clusterType, clusterName, clusterCrn);
            } else if (logging.getAdlsGen2() != null) {
                storageLocation = resolveAdlsGen2Location(storageLocation,
                        clusterType, clusterName, clusterCrn);
            } else if (logging.getGcs() != null) {
                storageLocation = resolveGcsLocation(storageLocation,
                        clusterType, clusterName, clusterCrn);
            } else {
                LOGGER.warn("None of the telemetry logging storage location was resolved, "
                        + "make sure storage type is set properly (currently supported: s3, abfs)");
            }
            logging.setStorageLocation(storageLocation);
        } else {
            LOGGER.debug("Telemetry is not set, skipping cloud storage location updates.");
        }
    }

    public String resolveS3Location(String location, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Start to resolve S3 storage location for telemetry (logging).");
        return s3ConfigGenerator.generateStoredLocation(location,
                clusterType, clusterName, Crn.fromString(clusterCrn).getResource());
    }

    public String resolveAdlsGen2Location(String location, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Start to resolve ADLS V2 storage location for telemetry (logging).");
        return adlsGen2ConfigGenerator.generateStoredLocation(location,
                clusterType, clusterName, Crn.fromString(clusterCrn).getResource());
    }

    public String resolveGcsLocation(String location, String clusterType,
            String clusterName, String clusterCrn) {
        LOGGER.debug("Start to resolve GCS storage location for telemetry (logging).");
        return gcsConfigGenerator.generateStoredLocation(location,
                clusterType, clusterName, Crn.fromString(clusterCrn).getResource());
    }

}
