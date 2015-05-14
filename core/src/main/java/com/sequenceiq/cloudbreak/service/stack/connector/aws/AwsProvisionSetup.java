package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class AwsProvisionSetup implements ProvisionSetup {

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) {
        return new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                .withSetupProperties(new HashMap<String, Object>());
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        return null;
    }

    public Map<String, Object> getSetupProperties(Stack stack) {
        return new HashMap<>();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
