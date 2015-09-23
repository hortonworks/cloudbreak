package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.ImageStatusResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;

public interface ProvisionSetup {

    ProvisionEvent setupProvisioning(Stack stack) throws Exception;

    ProvisionEvent prepareImage(Stack stack) throws Exception;

    ImageStatusResult checkImage(Stack stack) throws Exception;

    /**
     * @deprecated There is no pre-provision check in SPI, therefore this method will be deleted soon
     *
     *
     */
    @Deprecated
    String preProvisionCheck(Stack stack);

    CloudPlatform getCloudPlatform();

}
