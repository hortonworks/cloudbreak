package com.sequenceiq.cloudbreak.cloud.aws.resource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceBatchConfig;

@Service
public class AwsNativeGovResourceBatchConfig implements ResourceBatchConfig {

    @Value("${cb.aws.stopStart.batch.size}")
    private Integer stopStartBatchSize;

    @Value("${cb.aws.create.batch.size}")
    private Integer createBatchSize;

    public Integer stopStartBatchSize() {
        return stopStartBatchSize;
    }

    public Integer createBatchSize() {
        return createBatchSize;
    }

    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }
}