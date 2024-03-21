package com.sequenceiq.remoteenvironment.scheduled.archiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivateControlPlaneQueryConfig {

    @Value("${remoteenvironment.controlplane.query.intervalminutes:60}")
    private int intervalInMinutes;

    public int getIntervalInMintues() {
        return intervalInMinutes;
    }
}
