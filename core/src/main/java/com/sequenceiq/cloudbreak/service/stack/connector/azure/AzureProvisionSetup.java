package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureProvisionSetup implements ProvisionSetup {
    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "setupProvisioning"));
    }

    @Override
    public ProvisionEvent prepareImage(Stack stack) throws Exception {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "prepareImage"));
    }

    @Override
    public ImageStatusResult checkImage(Stack stack) throws Exception {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "checkImage"));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
