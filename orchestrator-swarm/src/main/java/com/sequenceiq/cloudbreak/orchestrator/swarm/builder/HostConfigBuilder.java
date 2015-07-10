package com.sequenceiq.cloudbreak.orchestrator.swarm.builder;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;

public class HostConfigBuilder {

    private HostConfig hostConfig;

    public HostConfigBuilder() {
        hostConfig = new HostConfig();
    }

    public HostConfigBuilder defaultConfig() {
        return hostNetwork().alwaysRestart().privileged();
    }

    public HostConfigBuilder hostNetwork() {
        hostConfig.setNetworkMode("host");
        return this;
    }

    public HostConfigBuilder alwaysRestart() {
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        return this;
    }

    public HostConfigBuilder privileged() {
        hostConfig.setPrivileged(true);
        return this;
    }

    public HostConfigBuilder expose(Integer... ports) {
        Ports exposedPorts = new Ports();
        for (Integer port : ports) {
            exposedPorts.add(new PortBinding(new Ports.Binding(port), new ExposedPort(port)));
        }
        hostConfig.setPortBindings(exposedPorts);
        return this;
    }

    public HostConfigBuilder binds(Bind[] binds) {
        hostConfig.setBinds(binds);
        return this;
    }

    public HostConfig build() {
        return hostConfig;
    }
}
