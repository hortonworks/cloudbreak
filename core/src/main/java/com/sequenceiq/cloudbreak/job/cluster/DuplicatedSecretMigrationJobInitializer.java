package com.sequenceiq.cloudbreak.job.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class DuplicatedSecretMigrationJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedSecretMigrationJobInitializer.class);

    @Override
    public void initJobs() {
    }
}
