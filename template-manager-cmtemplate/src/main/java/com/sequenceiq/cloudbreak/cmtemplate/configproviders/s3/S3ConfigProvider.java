package com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_DIR;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class S3ConfigProvider {

    private static final String S3GUARD_METADATASTORE_IMPL_PARAM = "fs.s3a.metadatastore.impl";

    private static final String S3GUARD_METADATASTORE_IMPL_VALUE = "org.apache.hadoop.fs.s3a.s3guard.DynamoDBMetadataStore";

    private static final String S3GUARD_TABLE_CREATE_PARAM = "fs.s3a.s3guard.ddb.table.create";

    private static final String S3GUARD_TABLE_CREATE_VALUE = "true";

    private static final String S3GUARD_TABLE_NAME_PARAM = "fs.s3a.s3guard.ddb.table";

    private static final String S3GUARD_TABLE_REGION_PARAM = "fs.s3a.s3guard.ddb.region";

    private static final String S3GUARD_AUTHORITATIVE_PATH_PARAM = "fs.s3a.authoritative.path";

    private static final String S3GUARD_TABLE_TAG_PARAM = "fs.s3a.s3guard.ddb.table.tag.";

    private static final String CDP_TABLE_ROLE = "cdp_table_role";

    private static final String S3GUARD_TABLE_TAG_VALUE = "s3guard";

    private static final String S3_BUCKET_ENDPOINT_PARAM_TEMPLATE = "fs.s3a.bucket.%s.endpoint";

    private static final String S3GUARD_DIRECTORY_MARKER_RETENTION_PARAM = "fs.s3a.directory.marker.retention";

    private static final String S3GUARD_DIRECTORY_MARKER_RETENTION_VALUE = "authoritative";

    private static final String S3_ENDPOINT_TEMPLATE = "s3.%s.amazonaws.com";

    @Inject
    private LocationHelper locationHelper;

    @Inject
    private EntitlementService entitlementService;

    public void getServiceConfigs(TemplatePreparationObject templatePreparationObject, StringBuilder hdfsCoreSiteSafetyValveValue) {
        if (isS3FileSystemConfigured(templatePreparationObject)) {
            configureS3GuardCoreSiteParameters(templatePreparationObject, hdfsCoreSiteSafetyValveValue);
            configureS3BucketLocationCoreSiteParameters(templatePreparationObject, hdfsCoreSiteSafetyValveValue);
        }
    }

    private boolean isS3FileSystemConfigured(TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && source.getFileSystemConfigurationView().get().getType().equals(FileSystemType.S3.name());
    }

    private void configureS3BucketLocationCoreSiteParameters(TemplatePreparationObject source, StringBuilder hdfsCoreSiteSafetyValveValue) {
        S3FileSystemConfigurationsView s3FileSystemConfigurationsView =
                (S3FileSystemConfigurationsView) source.getFileSystemConfigurationView().get();

        source.getPlacementView().ifPresent(placementView -> {
            Set<String> buckets = s3FileSystemConfigurationsView.getLocations().stream()
                    .map(loc -> locationHelper.parseS3BucketName(loc.getValue()))
                    .collect(Collectors.toSet());
            buckets.forEach(bucketName -> {
                String s3BucketEndpointParam = String.format(S3_BUCKET_ENDPOINT_PARAM_TEMPLATE, bucketName);
                String s3BucketEndpoint = String.format(S3_ENDPOINT_TEMPLATE, placementView.getRegion());
                hdfsCoreSiteSafetyValveValue.append(ConfigUtils.getSafetyValveProperty(s3BucketEndpointParam, s3BucketEndpoint));
            });
        });
    }

    private void configureS3GuardCoreSiteParameters(TemplatePreparationObject source, StringBuilder hdfsCoreSiteSafetyValveValue) {
        S3FileSystemConfigurationsView s3FileSystemConfigurationsView =
                (S3FileSystemConfigurationsView) source.getFileSystemConfigurationView().get();

        if (isS3GuardTableConfigured(s3FileSystemConfigurationsView)) {
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_METADATASTORE_IMPL_PARAM, S3GUARD_METADATASTORE_IMPL_VALUE));
            addTags(source, hdfsCoreSiteSafetyValveValue);
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_TABLE_CREATE_PARAM, S3GUARD_TABLE_CREATE_VALUE));
            if (entitlementService.isS3DirectoryMarkerRetentionEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
                hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                        .getSafetyValveProperty(S3GUARD_DIRECTORY_MARKER_RETENTION_PARAM, S3GUARD_DIRECTORY_MARKER_RETENTION_VALUE));
            }
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_TABLE_NAME_PARAM, s3FileSystemConfigurationsView.getS3GuardDynamoTableName()));

            source.getPlacementView().ifPresent(placementView -> hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_TABLE_REGION_PARAM, placementView.getRegion())));

            ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_DIR)
                    .ifPresent(location -> hdfsCoreSiteSafetyValveValue.append(
                            ConfigUtils.getSafetyValveProperty(S3GUARD_AUTHORITATIVE_PATH_PARAM, location.getValue())));
        }
    }

    private void addTags(TemplatePreparationObject source, StringBuilder hdfsCoreSiteSafetyValveValue) {
        for (Map.Entry<String, String> entry : source.getDefaultTags().entrySet()) {
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                    .getSafetyValveProperty(S3GUARD_TABLE_TAG_PARAM + entry.getKey(), entry.getValue()));
        }
        hdfsCoreSiteSafetyValveValue.append(ConfigUtils
                .getSafetyValveProperty(S3GUARD_TABLE_TAG_PARAM + CDP_TABLE_ROLE, S3GUARD_TABLE_TAG_VALUE));
    }

    private boolean isS3GuardTableConfigured(S3FileSystemConfigurationsView s3FileSystemConfigurationsView) {
        return StringUtils.isNotBlank(s3FileSystemConfigurationsView.getS3GuardDynamoTableName());
    }
}
