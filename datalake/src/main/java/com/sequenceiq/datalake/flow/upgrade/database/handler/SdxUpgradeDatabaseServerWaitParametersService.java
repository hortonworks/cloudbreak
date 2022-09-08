package com.sequenceiq.datalake.flow.upgrade.database.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SdxUpgradeDatabaseServerWaitParametersService {

    @Value("${sdx.upgrade.database.sleeptime-sec}")
    private long sleepTimeInSec;

    @Value("${sdx.upgrade.database.duration-min}")
    private long durationInMinutes;

    public long getSleepTimeInSec() {
        return sleepTimeInSec;
    }

    public long getDurationInMinutes() {
        return durationInMinutes;
    }
}
