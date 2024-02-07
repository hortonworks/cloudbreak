package com.sequenceiq.datalake.flow.imdupdate.handler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("sdx.update.imd")
public class SdxInstanceMetadataUpdateWaitConfiguration {

    private long sleepTimeSec;

    private long durationMin;

    public long getSleepTimeSec() {
        return sleepTimeSec;
    }

    public long getDurationMin() {
        return durationMin;
    }

    public void setSleepTimeSec(long sleepTimeSec) {
        this.sleepTimeSec = sleepTimeSec;
    }

    public void setDurationMin(long durationMin) {
        this.durationMin = durationMin;
    }
}
