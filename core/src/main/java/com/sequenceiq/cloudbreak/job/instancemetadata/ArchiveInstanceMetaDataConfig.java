package com.sequenceiq.cloudbreak.job.instancemetadata;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArchiveInstanceMetaDataConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveInstanceMetaDataConfig.class);

    @Value("${archiveinstancemetadata.intervalhours:24}")
    private int intervalInHours;

    @Value("${archiveinstancemetadata.archiveolderthanweeks:4}")
    private int archiveOlderThanWeeks;

    @Value("${archiveinstancemetadata.pageSize:400}")
    private int pageSize;

    @Value("${archiveinstancemetadata.enabled:true}")
    private boolean archiveInstanceMetaDataEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Archive InstanceMetaData is {}", archiveInstanceMetaDataEnabled ? "enabled" : "disabled");
    }

    public boolean isArchiveInstanceMetaDataEnabled() {
        return archiveInstanceMetaDataEnabled;
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
