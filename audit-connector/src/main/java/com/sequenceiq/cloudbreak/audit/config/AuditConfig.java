package com.sequenceiq.cloudbreak.audit.config;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.audit.converter.AttemptAuditEventResultBuilderUpdater;
import com.sequenceiq.cloudbreak.audit.converter.AuditEventBuilderUpdater;
import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;

@Configuration
public class AuditConfig {

    private static final int DEFAULT_AUDIT_PORT = 8982;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditConfig.class);

    @Value("${altus.audit.endpoint:}")
    private String endpoint;

    private String host;

    private int port;

    @Inject
    private StructuredEventEnablementConfig structuredEventEnablementConfig;

    @Inject
    private List<AuditEventBuilderUpdater> auditEventBuilderUpdaters;

    @Inject
    private List<AttemptAuditEventResultBuilderUpdater> attemptAuditEventResultBuilderUpdaters;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            String[] parts = endpoint.split(":");
            if (parts.length < 1 || parts.length > 2) {
                throw new IllegalArgumentException("altus.audit.endpoint must be in host or host:port format.");
            }
            host = parts[0];
            port = parts.length == 2
                    ? Integer.parseInt(parts[1])
                    : DEFAULT_AUDIT_PORT;
        } else if (structuredEventEnablementConfig.isAuditServiceEnabled()) {
            throw new IllegalStateException("Audit service is enabled, but altus.audit.endpoint is not configured");
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(endpoint);
    }

    @Bean
    public Map<Class, AuditEventBuilderUpdater> eventDataUpdaters() {
        Map<Class, AuditEventBuilderUpdater> result = new LinkedHashMap<>(auditEventBuilderUpdaters.size());
        auditEventBuilderUpdaters.forEach(updater -> result.put(updater.getType(), updater));
        if (MapUtils.isNotEmpty(result)) {
            String eventDataUpdaters = result.entrySet().stream().map(auditEventDataUpdaterEntry -> String.format("[%s :: %s]",
                    getIfNotNull(auditEventDataUpdaterEntry.getKey(), Class::getSimpleName),
                    getIfNotNull(auditEventDataUpdaterEntry.getValue(), u -> u.getClass().getSimpleName()))).collect(Collectors.joining(","));
            LOGGER.debug("The " + AuditEventBuilderUpdater.class.getSimpleName() + " has the following implementations: {}", eventDataUpdaters);
        } else {
            LOGGER.debug("The " + AuditEventBuilderUpdater.class.getSimpleName() + " has no any implementation!");
        }
        return result;
    }

    @Bean
    public Map<Class, AttemptAuditEventResultBuilderUpdater> auditEventDataUpdaters() {
        Map<Class, AttemptAuditEventResultBuilderUpdater> result = new LinkedHashMap<>(attemptAuditEventResultBuilderUpdaters.size());
        attemptAuditEventResultBuilderUpdaters.forEach(updater -> result.put(updater.getType(), updater));
        if (MapUtils.isNotEmpty(result)) {
            String auditEventDataUpdaters = result.entrySet().stream().map(auditEventDataUpdaterEntry -> String.format("[%s :: %s]",
                    getIfNotNull(auditEventDataUpdaterEntry.getKey(), Class::getSimpleName),
                    getIfNotNull(auditEventDataUpdaterEntry.getValue(), u -> u.getClass().getSimpleName()))).collect(Collectors.joining(","));
            LOGGER.debug("The " + AttemptAuditEventResultBuilderUpdater.class.getSimpleName() + " has the following implementations: {}",
                    auditEventDataUpdaters);
        } else {
            LOGGER.debug("The " + AttemptAuditEventResultBuilderUpdater.class.getSimpleName() + " has no any implementation!");
        }
        return result;
    }

}
