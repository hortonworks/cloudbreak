package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringUrlResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringUrlResolver.class);

    private static final String ACCOUNT_ID_TEMPLATE = "$accountid";

    private final String monitoringEndpointConfig;

    private final String monitoringPaasEndpointConfig;

    public MonitoringUrlResolver(String monitoringEndpointConfig, String monitoringPaasEndpointConfig) {
        this.monitoringEndpointConfig = monitoringEndpointConfig;
        if (StringUtils.isBlank(monitoringPaasEndpointConfig)) {
            this.monitoringPaasEndpointConfig = monitoringEndpointConfig;
        } else {
            this.monitoringPaasEndpointConfig = monitoringPaasEndpointConfig;
        }
    }

    public String resolve(String accountId, boolean saasEnabled) {
        String urlTemplate = saasEnabled ? monitoringEndpointConfig : monitoringPaasEndpointConfig;
        return resolve(accountId, urlTemplate);
    }

    public String resolve(String accountId, String urlTemplate) {
        if (StringUtils.isNoneBlank(urlTemplate, accountId)) {
            String url = urlTemplate.replace(ACCOUNT_ID_TEMPLATE, accountId);
            LOGGER.info("Generated monitoring URL {}.", url);
            return url;
        }
        LOGGER.info("Generated monitoring URL {}.", urlTemplate);
        return urlTemplate;
    }
}
