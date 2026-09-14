package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagKeyNormalizer;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class CloudPlatformTagKeyNormalizerProvider {

    private final GcpLabelUtil gcpLabelUtil;

    @Inject
    public CloudPlatformTagKeyNormalizerProvider(GcpLabelUtil gcpLabelUtil) {
        this.gcpLabelUtil = gcpLabelUtil;
    }

    public TagKeyNormalizer forPlatform(String cloudPlatform) {
        if (CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform)) {
            return gcpLabelUtil::transformLabelKeyOrValue;
        }
        return TagKeyNormalizer.IDENTITY;
    }
}
