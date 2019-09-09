package com.sequenceiq.cloudbreak.ccmimpl.altus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinaSshdManagementClientConfig {
    @Value("${altus.minasshdmgmt.client.polling_interval_ms:5000}")
    private int pollingIntervalMs;

    @Value("${altus.minasshdmgmt.client.timeout_ms:300000}")
    private int timeoutMs;

    @Value("${altus.minasshdmgmt.client.list_mina_sshd_services_page_size:100}")
    private int listMinaSshdServicesPageSize;

    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getListMinaSshdServicesPageSize() {
        return listMinaSshdServicesPageSize;
    }
}
