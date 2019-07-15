package com.sequenceiq.cloudbreak.fluent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class FluentConfigService {

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

    private static final String CLUSTER_TYPE_DISTROX = "distrox";

    private static final String CLUSTER_TYPE_SDX = "sdx";

    private static final String CLUSTER_LOG_PREFIX = "cluster-logs";

    private static final String S3_PROVIDER_PREFIX = "s3";

    private static final String DEFAULT_PROVIDER_PREFIX = "stdout";

    public FluentConfigView createFluentConfigs(Stack stack, Telemetry telemetry) {
        if (telemetry != null && telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            String clusterName = stack.getCluster().getName();
            String platform = stack.getCloudPlatform();
            StackType stackType = stack.getType();

            String clusterType = CLUSTER_TYPE_DISTROX;
            if (StackType.DATALAKE.equals(stackType)) {
                clusterType = CLUSTER_TYPE_SDX;
            }
            String s3bucket = null;
            String s3LogFolderName = null;
            boolean fluentEnabled = false;
            String providerPrefix = DEFAULT_PROVIDER_PREFIX;

            if (logging.getS3() != null) {
                fluentEnabled = true;

                BucketFolderPrefixPair bucketFolderPrefixPair = generateBucketFolderPrefixPair(logging.getStorageLocation());

                s3LogFolderName = Paths.get(CLUSTER_LOG_PREFIX, clusterType, clusterName).toString();
                if (StringUtils.isNotEmpty(bucketFolderPrefixPair.getFolderPrefix())) {
                    s3LogFolderName = Paths.get(bucketFolderPrefixPair.getFolderPrefix(), clusterType, clusterName).toString();
                }
                s3bucket = bucketFolderPrefixPair.getBucket();
                providerPrefix = S3_PROVIDER_PREFIX;
            }
            return new FluentConfigView.Builder()
                    .withEnabled(fluentEnabled)
                    .withS3LogFolderName(s3LogFolderName)
                    .withS3LogArchiveBucketName(s3bucket)
                    .withPlatform(platform)
                    .withProviderPrefix(providerPrefix)
                    .withOverrideAttributes(
                            logging.getAttributes() != null ? new HashMap<>(logging.getAttributes()) : new HashMap<>()
                    )
                    .build();
        } else {
            return new FluentConfigView.Builder()
                    .withEnabled(false)
                    .build();
        }
    }

    private BucketFolderPrefixPair generateBucketFolderPrefixPair(String location) {
        if (StringUtils.isNotEmpty(location)) {
            Path path = getPathWithoutSchemePrefixes(location, S3_SCHEME_PREFIXES);
            String[] splitted = path.toString().split("/", 2);
            if (splitted.length < 2) {
                return new BucketFolderPrefixPair(splitted[0], null);
            }
            return new BucketFolderPrefixPair(splitted[0], splitted[1]);
        }
        throw new CloudbreakServiceException("Location parameter is missing for S3");
    }

    private Path getPathWithoutSchemePrefixes(String input, String... schemePrefixes) {
        for (String schemePrefix : schemePrefixes) {
            if (input.startsWith(schemePrefix)) {
                String[] splitted = input.split(schemePrefix);
                if (splitted.length > 1) {
                    return Paths.get(splitted[1]);
                }
            }
        }
        return Paths.get(input);
    }

    private static class BucketFolderPrefixPair {
        private final String bucket;

        private final String folderPrefix;

        BucketFolderPrefixPair(String bucket, String folderPrefix) {
            this.bucket = bucket;
            this.folderPrefix = folderPrefix;
        }

        String getBucket() {
            return bucket;
        }

        String getFolderPrefix() {
            return folderPrefix;
        }
    }
}
