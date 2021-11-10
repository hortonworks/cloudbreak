package com.sequenceiq.cloudbreak.kudu.config;

import java.util.List;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;

public class KuduConfiguration extends AbstractStreamingConfiguration {

    private final List<String> servers;

    public KuduConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit, List<String> servers) {
        super(enabled, numberOfWorkers, queueSizeLimit);
        this.servers = servers;
    }

    public List<String> getServers() {
        return servers;
    }
}
