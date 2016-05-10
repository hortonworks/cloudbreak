package com.sequenceiq.cloudbreak.core.bootstrap.config;

import com.sequenceiq.cloudbreak.orchestrator.model.HostServiceConfig;

public class HostServiceConfigBuilder {
    private String name;

    private String version;

    private String repoUrl;

    public HostServiceConfigBuilder builder() {
        return this;
    }

    public HostServiceConfigBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public HostServiceConfigBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    public HostServiceConfigBuilder withRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
        return this;
    }

    public HostServiceConfig build() {
        return new HostServiceConfig(name, version, repoUrl);
    }

}
