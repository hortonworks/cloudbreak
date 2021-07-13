package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_VOLUME_ATTACHMENT_RESOURCE_BUILDER_ORDER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsAttachmentResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;

@Component
public class AwsNativeAttachmentResourceBuilder extends AwsAttachmentResourceBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeAttachmentResourceBuilder.class);

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws PreserveResourceException {
        LOGGER.debug("No need to delete volumes here as AwsNativeVolumeResourceBuilder will take care of it, for volume: '{}'", resource.getName());
        return null;
    }

    @Override
    public Variant variant() {
        return AWS_NATIVE_VARIANT.variant();
    }

    @Override
    public int order() {
        return NATIVE_VOLUME_ATTACHMENT_RESOURCE_BUILDER_ORDER;
    }
}
