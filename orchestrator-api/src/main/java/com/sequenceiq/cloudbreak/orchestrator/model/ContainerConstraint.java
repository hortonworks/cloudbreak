package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;

public class ContainerConstraint {

    private final String[] cmd;
    private final Double cpus;
    private final Double mem;
    private final Integer instances;
    private final List<Integer> ports;
    private final List<List<String>> constraints;
    private final Map<String, String> volumeBinds;
    private final List<String> env;
    private final String networkMode;
    private final TcpPortBinding tcpPortBinding;
    private final Map<String, String> privateIpsByHostname;
    private final String name;


    public ContainerConstraint(ContainerConstraint.Builder builder) {
        this.cmd = builder.cmd;
        this.cpus = builder.cpus;
        this.mem = builder.mem;
        this.instances = builder.instances;
        this.ports = builder.ports;
        this.constraints = builder.constraints;
        this.volumeBinds = builder.volumeBinds;
        this.env = builder.env;
        this.networkMode = builder.networkMode;
        this.tcpPortBinding = builder.tcpPortBinding;
        this.privateIpsByHostname = builder.privateIpsByHostname;
        this.name = builder.name;
    }

    public String[] getCmd() {
        return cmd;
    }

    public Double getCpus() {
        return cpus;
    }

    public Double getMem() {
        return mem;
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

    public List<String> getEnv() {
        return env;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public TcpPortBinding getTcpPortBinding() {
        return tcpPortBinding;
    }

    public Map<String, String> getPrivateIpsByHostname() {
        return privateIpsByHostname;
    }

    public String getName() {
        return name;
    }

    public static class Builder {

        private String[] cmd;
        private List<Integer> ports = new ArrayList<>();
        private Double cpus;
        private Double mem;
        private Integer instances;
        private List<List<String>> constraints = new ArrayList<>();
        private Map<String, String> volumeBinds = new HashMap<>();
        private List<String> env = new ArrayList<>();
        private String networkMode;
        private TcpPortBinding tcpPortBinding;
        private Map<String, String> privateIpsByHostname = new HashMap<>();
        private String name;

        public Builder containerConstraint(ContainerConstraint containerConstraint) {
            this.cmd = containerConstraint.getCmd();
            this.ports = containerConstraint.getPorts();
            this.cpus = containerConstraint.getCpus();
            this.mem = containerConstraint.getMem();
            this.instances = containerConstraint.getInstances();
            this.constraints = containerConstraint.getConstraints();
            this.volumeBinds = containerConstraint.getVolumeBinds();
            this.env = containerConstraint.getEnv();
            this.networkMode = containerConstraint.getNetworkMode();
            this.tcpPortBinding = containerConstraint.getTcpPortBinding();
            this.privateIpsByHostname = containerConstraint.getPrivateIpsByHostname();
            this.name = containerConstraint.getName();
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

        public Builder addEnv(List<String> env) {
            this.env.addAll(env);
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

        public Builder addPrivateIpsByHostname(Map<String, String> privateIpsByHostname) {
            this.privateIpsByHostname.putAll(privateIpsByHostname);
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public ContainerConstraint build() {
            return new ContainerConstraint(this);
        }
    }
}
