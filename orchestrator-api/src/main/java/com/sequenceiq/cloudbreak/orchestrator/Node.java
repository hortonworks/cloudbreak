package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Set;

public class Node {
    private String privateIp;
    private String publicIp;
    private String hostname;
    private Set<String> dataVolumes;

    public Node(String privateIp, String publicIp) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
    }

    public Node(String privateIp, String publicIp, String hostname, Set<String> dataVolumes) {
        this.privateIp = privateIp;
        this.publicIp = publicIp;
        this.hostname = hostname;
        this.dataVolumes = dataVolumes;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getHostname() {
        return hostname;
    }

    public Set<String> getDataVolumes() {
        return dataVolumes;
    }
}
