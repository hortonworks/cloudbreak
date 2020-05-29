package com.sequenceiq.cloudbreak.structuredevent.conf;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredEventEnablementConfig {

    @Value("${cb.kafka.bootstrap.servers:}")
    private String bootstrapServers;

    @Value("${cb.audit.filepath:}")
    private String auditFilePath;

    @Value("${audit.service.enabled}")
    private boolean auditServiceEnabled;

    @Value("${cb.kafka.structured.events.topic:StructuredEvents}")
    private String structuredEventsTopic;

    public boolean isKafkaConfigured() {
        return StringUtils.isNotEmpty(bootstrapServers);
    }

    public boolean isFilePathConfigured() {
        return StringUtils.isNotEmpty(auditFilePath);
    }

    public boolean isAuditServiceEnabled() {
        return auditServiceEnabled;
    }

    public String getAuditFilePath() {
        return auditFilePath;
    }

    public String getStructuredEventsTopic() {
        return structuredEventsTopic;
    }
}