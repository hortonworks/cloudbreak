package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.LoggingAttributesHolder;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.logging.CommonLoggingAttributes;
import com.sequenceiq.cloudbreak.cloud.model.logging.S3LoggingAttributes;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

/**
 * Decorate fluentd related salt pillar configs (in order to ship daemon logs to cloud storage)
 * Currently only S3 output supported, right now salt properties are filled based on attributes,
 * the calculation can be changed based on UI requirements.
 * The defaults could look like this:
 * <pre>
 * fluent:
 *   enabled: false
 *   user: root
 *   group: root
 *   providerPrefix: "stdout"
 *   partitionIntervalMin: 5
 *   s3LogArchiveBucketName:
 *   s3LogFolderName:
 * </pre>
 */
public class TelemetryDecorator {

    private static final String TD_AGENT_USER_DEFAULT = "root";

    private static final String TD_AGENT_GROUP_DEFAULT = "root";

    // Right now it is built-in, add a way to override log folder configuration
    // if custom log locations will be supported by the cluster template with CB deploy
    private static final String LOGS_FOLDER_PREFIX_DEFAULT = "/var/log";

    private static final Integer PARTITION_INTERVAL_MIN_DEFAULT = 5;

    private static final String CLUSTER_TYPE_DISTROX = "distrox";

    private static final String CLUSTER_TYPE_SDX = "sdx";

    private static final String CLUSTER_LOG_PREFIX = "cluster-logs";

    private static final String FLUENT_ENABLED_PROPERTY = "enabled";

    private static final String FLUENT_USER_PROPERTY = "user";

    private static final String FLUENT_GROUP_PROPERTY = "group";

    private static final String FLUENT_PROVIDER_PREFIX_PROPERTY = "providerPrefix";

    private static final String FLUENT_SERVER_LOG_PREFIX_PROPERTY = "serverLogFolderPrefix";

    private static final String FLUENT_AGENT_LOG_PREFIX_PROPERTY = "agentLogFolderPrefix";

    private static final String FLUENT_SERVICE_LOG_PREFIX_PROPERTY = "serviceLogFolderPrefix";

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

    private static final String S3_PARTITION_INTERVAL_PROPERTY = "partitionIntervalMin";

    private static final String S3_LOG_ARCHIVE_BUCKET_PROPERTY = "s3LogArchiveBucketName";

    private static final String S3_LOG_FOLDER_NAME_PROPERTY = "s3LogFolderName";

    private final Map<String, SaltPillarProperties> servicePillar;

    public TelemetryDecorator(Map<String, SaltPillarProperties> servicePillar) {
        this.servicePillar = servicePillar;
    }

    public void decoratePillar(Telemetry telemetry, String clusterName, StackType stackType) {
        if (telemetry != null) {
            Logging logging = telemetry.getLogging();
            if (logging != null && logging.isEnabled() && logging.getOutputType() != null) {
                if (logging.getAttributes() != null) {
                    Map<String, Object> fluentConfig = new HashMap<>();
                    fillFluentConfigs(logging, fluentConfig, clusterName, stackType);
                }
            }
        }
    }

    private void fillFluentConfigs(Logging logging, Map<String, Object> fluentConfig,
            String clusterName, StackType stackType) {
        LoggingAttributesHolder attributes = logging.getAttributes();
        if (attributes != null) {
            String user = TD_AGENT_USER_DEFAULT;
            String group = TD_AGENT_GROUP_DEFAULT;
            CommonLoggingAttributes common = attributes.getCommonAttributes();
            if (common != null) {
                if (StringUtils.isNotEmpty(common.getUser())) {
                    user = common.getUser();
                }
                if (StringUtils.isNotEmpty(common.getGroup())) {
                    group = common.getGroup();
                }
            }
            fluentConfig.put(FLUENT_USER_PROPERTY, user);
            fluentConfig.put(FLUENT_GROUP_PROPERTY, group);

            fluentConfig.put(FLUENT_SERVER_LOG_PREFIX_PROPERTY, LOGS_FOLDER_PREFIX_DEFAULT);
            fluentConfig.put(FLUENT_AGENT_LOG_PREFIX_PROPERTY, LOGS_FOLDER_PREFIX_DEFAULT);
            fluentConfig.put(FLUENT_SERVICE_LOG_PREFIX_PROPERTY, LOGS_FOLDER_PREFIX_DEFAULT);
            fluentConfig.put(FLUENT_ENABLED_PROPERTY, true);

            if (LoggingOutputType.S3.equals(logging.getOutputType()) && attributes.getS3Attributes() != null) {
                S3LoggingAttributes s3Attributes = attributes.getS3Attributes();
                fluentConfig.put(FLUENT_PROVIDER_PREFIX_PROPERTY, "s3");
                fluentConfig.put(S3_PARTITION_INTERVAL_PROPERTY, defaultIfNull(s3Attributes.getPartitionIntervalMin(), PARTITION_INTERVAL_MIN_DEFAULT));
                calculateS3BucketAndLogFolder(clusterName, stackType, s3Attributes, fluentConfig);
            } else {
                fluentConfig.put(FLUENT_ENABLED_PROPERTY, false);
            }
            servicePillar.put("fluent",
                    new SaltPillarProperties("/fluent/init.sls", singletonMap("fluent", fluentConfig)));
        }
    }

    @VisibleForTesting
    void calculateS3BucketAndLogFolder(String clusterName, StackType stackType,
            S3LoggingAttributes attributes, Map<String, Object> fluentConfig) {
        String bucket = attributes.getBucket();
        String basePath = attributes.getBasePath();
        BucketFolderPrefixPair bucketFolderPrefixPair = generateBucketFolderPrefixPair(bucket, basePath);

        String clusterType = CLUSTER_TYPE_DISTROX;
        if (StackType.DATALAKE.equals(stackType)) {
            clusterType = CLUSTER_TYPE_SDX;
        }
        String s3LogFolderName = Paths.get(CLUSTER_LOG_PREFIX, clusterType, clusterName).toString();
        if (StringUtils.isNotEmpty(bucketFolderPrefixPair.getFolderPrefix())) {
            s3LogFolderName = Paths.get(bucketFolderPrefixPair.getFolderPrefix(), clusterType, clusterName).toString();
        }

        fluentConfig.put(S3_LOG_ARCHIVE_BUCKET_PROPERTY, bucketFolderPrefixPair.getBucket());
        fluentConfig.put(S3_LOG_FOLDER_NAME_PROPERTY, s3LogFolderName);
    }

    private BucketFolderPrefixPair generateBucketFolderPrefixPair(String bucket, String basePath) {
        if (StringUtils.isNoneEmpty(bucket, basePath)) {
            Path bucketPath = getPathWithoutSchemePrefixes(bucket, S3_SCHEME_PREFIXES);
            String[] splittedBucket = bucketPath.toString().split("/", 2);
            return new BucketFolderPrefixPair(splittedBucket[0], basePath);
        } else if (StringUtils.isNotEmpty(bucket)) {
            Path path = getPathWithoutSchemePrefixes(bucket, S3_SCHEME_PREFIXES);
            String[] splitted = path.toString().split("/");
            return new BucketFolderPrefixPair(splitted[0], null);
        } else if (StringUtils.isNotEmpty(basePath)) {
            Path path = getPathWithoutSchemePrefixes(basePath, S3_SCHEME_PREFIXES);
            String[] splitted = path.toString().split("/", 2);
            if (splitted.length < 2) {
                return new BucketFolderPrefixPair(splitted[0], null);
            }
            return new BucketFolderPrefixPair(splitted[0], splitted[1]);
        }
        throw new CloudbreakServiceException("Bucket and / or basePath parameters are missing for S3 attributes");
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
