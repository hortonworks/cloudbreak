package com.sequenceiq.remoteenvironment.scheduled;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivateControlPlaneQueryConfig {

    @Value("${remoteenvironment.controlplane.query.intervalminutes:3}")
    private int intervalInMinutes;

    @Value("${remoteenvironment.controlplane.query.delayedfirststartminutes:5}")
    private int delayedFirstStartInMinutes;

    public int getIntervalInMintues() {
        return intervalInMinutes;
    }

    public int getDelayedFirstStartInMinutes() {
        return delayedFirstStartInMinutes;
    }
}
