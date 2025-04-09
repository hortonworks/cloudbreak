package com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.S3ExpressBucketValidator;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class S3ConfigProvider {

    private static final String S3_BUCKET_ENDPOINT_PARAM_TEMPLATE = "fs.s3a.bucket.%s.endpoint";

    private static final String S3_EXPRESS_BUCKET_ENDPOINT_PARAM_TEMPLATE = "fs.s3a.bucket.%s.endpoint.region";

    private static final String S3_ENDPOINT_TEMPLATE = "s3.%s.amazonaws.com";

    private static final String S3_EXPRESS_ENDPOINT_TEMPLATE = "%s";

    private static final String S3_FIPS_ENDPOINT_TEMPLATE = "s3-fips.%s.amazonaws.com";

    @Inject
    private LocationHelper locationHelper;

    @Inject
    private S3ExpressBucketValidator s3ExpressBucketValidator;

    public void getServiceConfigs(TemplatePreparationObject templatePreparationObject, StringBuilder hdfsCoreSiteSafetyValveValue) {
        if (isS3FileSystemConfigured(templatePreparationObject)) {
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
                hdfsCoreSiteSafetyValveValue.append(getHdfsCoreSiteSafetyValveValueForBucket(bucketName, source, placementView));
            });
        });
    }

    private String getHdfsCoreSiteSafetyValveValueForBucket(String bucketName, TemplatePreparationObject source, PlacementView placementView) {
        String s3BucketEndpointParam;
        String endpointFormat;
        String s3BucketEndpoint;
        if (s3ExpressBucketValidator.isS3ExpressBucket(bucketName)) {
            if (s3ExpressBucketValidator.validateVersionForS3ExpressBucket(source) && !source.getGeneralClusterConfigs().isGovCloud()) {
                s3BucketEndpointParam = String.format(S3_EXPRESS_BUCKET_ENDPOINT_PARAM_TEMPLATE, bucketName);
                s3BucketEndpoint = String.format(S3_EXPRESS_ENDPOINT_TEMPLATE, placementView.getRegion());
            } else {
                throw new RuntimeException("S3 Express buckets are only supported for non-gov CDH versions >= 7.2.18");
            }
        } else {
            s3BucketEndpointParam = String.format(S3_BUCKET_ENDPOINT_PARAM_TEMPLATE, bucketName);
            endpointFormat = source.getGeneralClusterConfigs().isGovCloud() ? S3_FIPS_ENDPOINT_TEMPLATE : S3_ENDPOINT_TEMPLATE;
            s3BucketEndpoint = String.format(endpointFormat, placementView.getRegion());
        }
        return ConfigUtils.getSafetyValveProperty(s3BucketEndpointParam, s3BucketEndpoint);
    }
}