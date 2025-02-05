package com.sequenceiq.cloudbreak.cloud.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceCreateBatchConfig;

@Service
public class AwsNativeResourceCreateBatchConfig implements ResourceCreateBatchConfig {

    @Value("${cb.aws.create.batch.size}")
    private Integer createBatchSize;

    public Integer batchSize() {
        return createBatchSize;
    }

    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }
}