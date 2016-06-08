package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Set;

public class Node {
    private String privateIp;
    private String publicIp;
    private String hostname;
    private String hostGroup;
    private Set<String> dataVolumes;

    public Node(String privateIp, String publicIp, String fqdn) {
        this(privateIp, publicIp);
        this.hostname = fqdn;
    }

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

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Node{");
        sb.append("privateIp='").append(privateIp).append('\'');
        sb.append(", publicIp='").append(publicIp).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", hostGroup='").append(hostGroup).append('\'');
        sb.append(", dataVolumes=").append(dataVolumes);
        sb.append('}');
        return sb.toString();
    }
}
