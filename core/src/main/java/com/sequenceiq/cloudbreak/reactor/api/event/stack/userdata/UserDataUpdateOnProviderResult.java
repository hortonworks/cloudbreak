package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public class UserDataUpdateOnProviderResult extends CloudPlatformResult implements Selectable {
    public UserDataUpdateOnProviderResult(Long resourceId) {
        super(resourceId);
    }
}
