package com.sequenceiq.environment.featureswitch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.featureswitch.FeatureSwitch;

@Component
public class AzureSingleResourceGroupFeatureSwitch implements FeatureSwitch {

    @Value("${cb.featureswitch.azure.single.resourcegroup}")
    private boolean active;

    @Override
    public boolean isActive() {
        return active;
    }
}
