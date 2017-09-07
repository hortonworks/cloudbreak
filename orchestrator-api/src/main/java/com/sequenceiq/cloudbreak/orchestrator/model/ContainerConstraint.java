package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;

public class ContainerConstraint {

    private final String[] cmd;

    private final Integer instances;

    private final List<Integer> ports;

    private final List<List<String>> constraints;

    private final Map<String, String> env;

    private final String networkMode;

    private final TcpPortBinding tcpPortBinding;

    private final ContainerName containerName;

    private final Map<String, String> links;

    private final String appName;

    private final List<String> hosts;

    private final Map<String, String> volumeBinds;

    private final Double cpu;

    private final Double mem;

    private final Double disk;

    private final String identifier;

    private ContainerConstraint(Builder builder) {
        cmd = builder.cmd;
        cpu = builder.cpus;
        mem = builder.mem;
        instances = builder.instances;
        ports = builder.ports;
        constraints = builder.constraints;
        volumeBinds = builder.volumeBinds;
        env = builder.env;
        networkMode = builder.networkMode;
        tcpPortBinding = builder.tcpPortBinding;
        hosts = builder.hosts;
        containerName = builder.containerName;
        links = builder.links;
        appName = builder.appName;
        disk = builder.disk;
        identifier = builder.identifier;
    }

    public String[] getCmd() {
        return cmd;
    }

    public Integer getInstances() {
        return instances;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public List<List<String>> getConstraints() {
        return constraints;
    }

    public Map<String, String> getVolumeBinds() {
        return volumeBinds;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public TcpPortBinding getTcpPortBinding() {
        return tcpPortBinding;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public Double getCpu() {
        return cpu;
    }

    public Double getMem() {
        return mem;
    }

    public ContainerName getContainerName() {
        return containerName;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public String getAppName() {
        return appName;
    }

    public Double getDisk() {
        return disk;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static class Builder {

        private String[] cmd;

        private List<Integer> ports = new ArrayList<>();

        private Double cpus;

        private Double mem;

        private Integer instances;

        private List<List<String>> constraints = new ArrayList<>();

        private Map<String, String> volumeBinds = new HashMap<>();

        private Map<String, String> env = new HashMap<>();

        private String networkMode;

        private TcpPortBinding tcpPortBinding;

        private List<String> hosts = new ArrayList<>();

        private ContainerName containerName;

        private Map<String, String> links = new HashMap<>();

        private String appName;

        private Double disk;

        private String identifier;

        public Builder containerConstraint(ContainerConstraint containerConstraint) {
            cmd = containerConstraint.getCmd();
            ports = containerConstraint.getPorts();
            cpus = containerConstraint.getCpu();
            mem = containerConstraint.getMem();
            instances = containerConstraint.getInstances();
            constraints = containerConstraint.getConstraints();
            volumeBinds = containerConstraint.getVolumeBinds();
            env = containerConstraint.getEnv();
            networkMode = containerConstraint.getNetworkMode();
            tcpPortBinding = containerConstraint.getTcpPortBinding();
            hosts = containerConstraint.getHosts();
            containerName = containerConstraint.getContainerName();
            links = containerConstraint.getLinks();
            appName = containerConstraint.getAppName();
            disk = containerConstraint.getDisk();
            identifier = containerConstraint.getIdentifier();
            return this;
        }

        public Builder cmd(String[] cmd) {
            this.cmd = cmd;
            return this;
        }

        public Builder ports(List<Integer> ports) {
            this.ports.addAll(ports);
            return this;
        }

        public Builder cpus(Double numberOfCpus) {
            cpus = numberOfCpus;
            return this;
        }

        public Builder memory(Double megaBytesOfMemory) {
            mem = megaBytesOfMemory;
            return this;
        }

        public Builder withDiskSize(Double diskSize) {
            disk = diskSize;
            return this;
        }

        public Builder instances(Integer numberOfInstances) {
            instances = numberOfInstances;
            return this;
        }

        public Builder constraints(List<List<String>> constraints) {
            this.constraints.addAll(constraints);
            return this;
        }

        public Builder addVolumeBindings(Map<String, String> volumeBinds) {
            this.volumeBinds.putAll(volumeBinds);
            return this;
        }

        public Builder addEnv(Map<String, String> env) {
            this.env.putAll(env);
            return this;
        }

        public Builder networkMode(String networkMode) {
            this.networkMode = networkMode;
            return this;
        }

        public Builder tcpPortBinding(TcpPortBinding binding) {
            tcpPortBinding = binding;
            return this;
        }

        public Builder addHosts(List<String> hosts) {
            this.hosts.addAll(hosts);
            return this;
        }

        public Builder withNamePrefix(String namePrefix) {
            containerName = new ContainerName(null, namePrefix);
            return this;
        }

        public Builder withName(String name) {
            containerName = new ContainerName(name, null);
            return this;
        }

        public Builder addLink(String hostContainerLink, String link) {
            links.put(hostContainerLink, link);
            return this;
        }

        public Builder withAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public ContainerConstraint build() {
            return new ContainerConstraint(this);
        }
    }
}
