package com.sequenceiq.cloudbreak.featureswitch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AzureSingleResourceGroupFeatureSwitch implements FeatureSwitch {

    @Value("${cb.featureswitch.azure.single.resourcegroup:false}")
    private boolean active;

    @Override
    public boolean isActive() {
        return active;
    }
}
