package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionRequest extends ProvisionEvent {

    private List<Long> stackIds = new ArrayList<>();

    public ProvisionRequest(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
