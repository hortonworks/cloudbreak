package com.sequenceiq.cloudbreak.job.archive;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArchiveClusterComponentConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveClusterComponentConfig.class);

    @Value("${archiveclustercomponent.intervalhours:24}")
    private int intervalInHours;

    @Value("${archiveclustercomponent.archiveolderthanweeks:4}")
    private int archiveOlderThanWeeks;

    @Value("${archiveclustercomponent.pageSize:400}")
    private int pageSize;

    @Value("${archiveclustercomponent.enabled:true}")
    private boolean archiveClusterComponentEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Archive InstanceMetaData is {}", archiveClusterComponentEnabled ? "enabled" : "disabled");
    }

    public boolean isArchiveClusterComponentEnabled() {
        return archiveClusterComponentEnabled;
    }

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getArchiveOlderThanWeeks() {
        return archiveOlderThanWeeks;
    }

    public int getPageSize() {
        return pageSize;
    }

}
