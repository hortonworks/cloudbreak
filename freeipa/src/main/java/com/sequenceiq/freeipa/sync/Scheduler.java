package com.sequenceiq.freeipa.sync;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class Scheduler {

    @Inject
    private FreeipaSyncService freeipaSyncService;

    @Scheduled(fixedDelay = 60000)
    public void sch() {
        freeipaSyncService.sync();
    }
}
