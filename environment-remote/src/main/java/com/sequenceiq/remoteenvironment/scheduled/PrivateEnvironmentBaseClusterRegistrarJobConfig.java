package com.sequenceiq.remoteenvironment.scheduled;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivateEnvironmentBaseClusterRegistrarJobConfig {

    @Value("${remoteenvironment.base.cluster.registrar.enabled:true}")
    private boolean enabled;

    @Value("${remoteenvironment.base.cluster.registrar.intervalminutes:10}")
    private int intervalInMinutes;

    @Value("${remoteenvironment.base.cluster.registrar.delayedfirststartminutes:10}")
    private int delayedFirstStartInMinutes;

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalInMintues() {
        return intervalInMinutes;
    }

    public int getDelayedFirstStartInMinutes() {
        return delayedFirstStartInMinutes;
    }
}
