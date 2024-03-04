package com.sequenceiq.remoteenvironment.scheduled.archiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteControlPlaneQueryConfig {

    @Value("${privateenvironment.controlplane.query.intervalminutes:5}")
    private int intervalInMinutes;

    public int getIntervalInMintues() {
        return intervalInMinutes;
    }
}
