package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.rootdisk;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsGovRootDiskTaggingResourceBuilder extends AwsNativeRootVolumeResourceBuilder {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }
}

