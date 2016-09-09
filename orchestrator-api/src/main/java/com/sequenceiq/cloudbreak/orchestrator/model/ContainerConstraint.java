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
    private Map<String, String> env;
    private final String networkMode;
    private final TcpPortBinding tcpPortBinding;
    private ContainerName containerName;
    private final Map<String, String> links;
    private final String appName;

    private List<String> hosts;
    private Map<String, String> volumeBinds;
    private Double cpu;
    private Double mem;
    private Double disk;

    private ContainerConstraint(ContainerConstraint.Builder builder) {
        this.cmd = builder.cmd;
        this.cpu = builder.cpus;
        this.mem = builder.mem;
        this.instances = builder.instances;
        this.ports = builder.ports;
        this.constraints = builder.constraints;
        this.volumeBinds = builder.volumeBinds;
        this.env = builder.env;
        this.networkMode = builder.networkMode;
        this.tcpPortBinding = builder.tcpPortBinding;
        this.hosts = builder.hosts;
        this.containerName = builder.containerName;
        this.links = builder.links;
        this.appName = builder.appName;
        this.disk = builder.disk;
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

        public Builder containerConstraint(ContainerConstraint containerConstraint) {
            this.cmd = containerConstraint.getCmd();
            this.ports = containerConstraint.getPorts();
            this.cpus = containerConstraint.getCpu();
            this.mem = containerConstraint.getMem();
            this.instances = containerConstraint.getInstances();
            this.constraints = containerConstraint.getConstraints();
            this.volumeBinds = containerConstraint.getVolumeBinds();
            this.env = containerConstraint.getEnv();
            this.networkMode = containerConstraint.getNetworkMode();
            this.tcpPortBinding = containerConstraint.getTcpPortBinding();
            this.hosts = containerConstraint.getHosts();
            this.containerName = containerConstraint.getContainerName();
            this.links = containerConstraint.getLinks();
            this.appName = containerConstraint.getAppName();
            this.disk = containerConstraint.getDisk();
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
            this.cpus = numberOfCpus;
            return this;
        }

        public Builder memory(Double megaBytesOfMemory) {
            this.mem = megaBytesOfMemory;
            return this;
        }

        public Builder withDiskSize(Double diskSize) {
            this.disk = diskSize;
            return this;
        }

        public Builder instances(Integer numberOfInstances) {
            this.instances = numberOfInstances;
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
            this.tcpPortBinding = binding;
            return this;
        }

        public Builder addHosts(List<String> hosts) {
            this.hosts.addAll(hosts);
            return this;
        }

        public Builder withNamePrefix(String namePrefix) {
            this.containerName = new ContainerName(null, namePrefix);
            return this;
        }

        public Builder withName(String name) {
            this.containerName = new ContainerName(name, null);
            return this;
        }

        public Builder addLink(String hostContainerLink, String link) {
            this.links.put(hostContainerLink, link);
            return this;
        }

        public Builder withAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public ContainerConstraint build() {
            return new ContainerConstraint(this);
        }
    }
}
